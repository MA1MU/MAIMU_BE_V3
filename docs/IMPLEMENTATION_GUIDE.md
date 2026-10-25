# MAIMU 성능 고도화 — 구현·테스트 완전 이해 가이드

> 이 문서의 목적: **내가 직접 설명할 수 있게** 만드는 것. 코드 한 줄, 테스트 한 번, 수치 하나까지
> "왜 이렇게 했는지"를 따라갈 수 있도록 적는다. 결과 요약은 [`PERFORMANCE_REPORT.md`](PERFORMANCE_REPORT.md)에 있고,
> 이 문서는 그 결과가 **어떤 코드에서, 어떤 절차로** 나왔는지를 다룬다.
>
> 읽는 순서: 1장(큰 그림) → 2장(문제를 눈으로 확인) → 3~5장(코드) → 6~7장(테스트·재현) → 8장(면접 대비).

---

## 목차

1. 5분 요약 — 무엇을, 왜, 어떻게
2. 개선 전 구조와 "느린 지점"을 숫자로 잡기
3. [코드] Write-back — 쓰기를 접수와 반영으로 분리
4. [코드] Cache-aside — 읽기 캐시
5. [코드] SSE — 실시간 알림
6. [테스트] k6 부하 스크립트의 구조와 의미
7. [재현] 내 손으로 처음부터 다시 측정하는 법
8. 면접 예상 질문 — 내 답변 스크립트
9. 용어 사전

---

## 1. 5분 요약 — 무엇을, 왜, 어떻게

### 서비스 한 줄 설명
MAIMU는 새 학기 첫인상 공유 서비스다. 회원이 **사물함(그룹)** 을 만들어 **초대 링크**를 뿌리면,
링크를 받은 사람은 **로그인 없이** 익명 쪽지(**마이무**)를 남긴다. 주인은 사물함에서 쪽지를 열어본다.

### 트래픽이 몰리는 구조
초대 링크가 단톡방·인스타에 퍼지면 **한 사람의 사물함 하나에** 수십~수백 명이 동시에 몰린다.
즉 부하가 고르게 퍼지지 않고 **"인기 키 하나(hot key)"** 에 집중된다. 이게 이 프로젝트 성능의 핵심 조건이다.

### 세 가지 개선과 각각의 한 줄 이유

| 개선 | 한 줄 이유 | 핵심 파일 |
| --- | --- | --- |
| **Write-back** | 쪽지를 쓸 때마다 DB에 커밋하면 느리다 → Redis에 쌓고 0.2초마다 묶어서 저장 | `MaimuWriteBackBuffer.java` |
| **Cache-aside** | 같은 사물함을 수백 명이 읽는다 → 요약 정보를 Redis에 캐시 | `GuestGroupCache.java` |
| **SSE** | 새 쪽지를 최대 1시간 뒤 메일로만 알던 것을 실시간 화면 알림으로 | `SseEmitters.java`, `NotificationController.java` |

### "단계를 나눈 것"이 이 프로젝트의 핵심 스토리
한 번에 다 바꾸면 "Redis 덕분"이라고밖에 말 못 한다. 그래서 **세 버전으로 나눠 측정**했다.

- **v0** = 원본 (아무것도 안 바꿈)
- **v1** = 불필요한 **락과 전체 로딩만 제거** (Redis 안 씀, 쿼리만 손봄)
- **v2** = v1 + **Redis Write-back / Cache-aside / SSE**

→ 이렇게 하면 "락 제거가 처리량을 X만큼, write-back이 지연을 Y만큼 줄였다"고 **인과를 쪼개서** 말할 수 있다.
면접에서 "그거 그냥 Redis 붙인 거 아니냐"는 질문에 "아니요, 측정해 보니 가장 큰 병목은 락이었고…"로 받을 수 있다.

---

## 2. 개선 전 구조와 "느린 지점"을 숫자로 잡기

성능 작업의 첫 단계는 코드를 고치는 게 아니라 **느린 곳을 숫자로 특정**하는 것이다.

### 2-1. 쪽지 작성(쓰기)이 느렸던 이유

개선 전 `GuestService.addMaimu`가 하던 일(의사코드):

```
[트랜잭션 시작]
 1) SELECT * FROM lockers WHERE group_id = ? FOR UPDATE   ← 사물함 행에 "배타 락"
 2) group.addMaimu(...)  →  INSERT INTO maimus ...         ← 커밋 시 반영
[커밋]  ← 이때 디스크에 fsync (느림)
```

여기서 **배타 락(`FOR UPDATE`)** 이 문제다. 쪽지(자식)를 넣는 데 사물함(부모)을 잠글 이유가 없다.
외래 키(FK)가 이미 "그 사물함이 존재한다"를 보장하기 때문이다. 그런데 이 락 때문에
**같은 사물함으로 가는 모든 쓰기가 한 줄로 선다(직렬화).**

**이걸 어떻게 "증명"했나** — 같은 코드로 두 가지를 측정했다:

| 측정 | TPS | 해석 |
| --- | ---: | --- |
| 사물함 50개에 쪽지를 고루 분산 | 461 | 락이 서로 다른 행이라 경합 없음 |
| 사물함 1개에 쪽지를 집중 | 63.9 | 같은 행 락을 두고 7배 느려짐 |

→ "분산하면 빠르고 집중하면 느리다"는 사실 자체가 **병목이 락 경합**이라는 증거다.

### 2-2. 링크 조회(읽기)가 느렸던 이유

개선 전 `GuestService.enterGroup`:

```java
Group group = groupRepository.findByIdWithLock(...)   // 읽기인데 FOR UPDATE 배타 락
...
this.maimuCount = group.getMaimus().size();           // 쪽지 전체를 메모리로 로딩해서 개수를 셈
```

두 가지 문제:
1. **읽기 요청인데 배타 락** → 읽기끼리도 직렬화
2. 개수만 필요한데 **쪽지 본문(TEXT) 전체를 N행 로딩** → 쪽지가 많을수록 느려짐

### 2-3. 알림이 느렸던 이유
`EmailSchedule`이 09~22시 매시 정각에만 메일을 보냈다. 앱을 보고 있어도 새로고침 전엔 알 수 없었다.

---

## 3. [코드] Write-back — 쓰기를 접수와 반영으로 분리

### 3-1. 전체 흐름 그림

```
[게스트의 요청 스레드]
  POST /guest/{groupId}/{token}/add
    → 초대 토큰 검증 (Redis 조회)
    → RPUSH maimu:write-buffer  (쪽지 1건을 JSON으로 Redis List 꼬리에 추가)
    → 201 응답                   ← 여기서 끝. DB는 건드리지 않음

[백그라운드 스케줄러, 0.2초마다]  ← MaimuWriteBackBuffer.flush()
    → LRANGE 0..999  (버퍼 앞에서 최대 1000건 꺼내보기)
    → 사물함·주인 일괄 조회 (IN 쿼리 1번)
    → multi-row INSERT 1번  (1000건을 INSERT 문 하나로)
    → LTRIM  (성공한 만큼만 버퍼 앞을 잘라냄)
    → 사물함 주인에게 SSE 알림 + 조회 캐시 무효화
```

핵심 아이디어 두 개:
- **"쓰기 요청"과 "DB 반영"을 떼어낸다.** 요청은 Redis에 넣기만 하고 즉시 응답 → 응답이 빠르다.
- **여러 건을 모아 한 번에 INSERT** → DB 커밋(fsync) 횟수가 1/N로 줄어든다.

### 3-2. 요청 경로 — `GuestService.addMaimu` + `push`

```java
// GuestService.java — DB 트랜잭션 없이 Redis 버퍼에만 넣고 응답한다.
public void addMaimu(String token, GuestRequest guestRequest){
    Invitation invitation = verifyInvitation(token);       // Redis에서 토큰 → groupId
    maimuWriteBackBuffer.push(invitation.getGroupId(), guestRequest);
}
```

```java
// MaimuWriteBackBuffer.java
public void push(Long groupId, GuestRequest request) {
    boolean anonymous = request.isAnonymous();
    PendingMaimu pending = new PendingMaimu(
            groupId,
            request.getTitle(),
            request.getMessage(),
            MaimuColor.fromString(request.getMaimuColor()).name(), // ① 잘못된 색은 여기서 거른다
            anonymous ? "익명" : request.getWriterName(),           // ② 익명 처리를 적재 시점에 확정
            anonymous,
            request.getSugarContent(),
            LocalDateTime.now());                                  // ③ 작성 시각 = 요청 시각
    redisTemplate.opsForList().rightPush(BUFFER_KEY, toJson(pending));  // RPUSH
}
```

**왜 이렇게 했는지 (번호 대응):**
- ① 색 문자열(`RED`/`YELLOW`/`GREEN`)이 잘못됐으면 **버퍼에 넣기 전에** 예외를 던져 400으로 돌려준다.
  버퍼에 잘못된 값이 들어가면 나중에 플러시 때 터져서 원인 찾기가 어렵다. "실패는 가능한 한 일찍."
- ② 익명이면 작성자 이름을 "익명"으로 **지금** 바꾼다. 나중 단계로 미루지 않는다.
- ③ `created_at`을 **요청 시각**으로 박는다. 만약 DB가 INSERT할 때의 시각을 쓰면, 버퍼에 머문 시간만큼
  작성 시각이 밀려 "방금 쓴 쪽지가 몇 초 전으로 보이는" 문제가 생긴다.

`PendingMaimu`는 버퍼에 넣을 때의 데이터 묶음이다. `record`(불변 데이터 클래스)로 두고 JSON으로 직렬화한다:
```java
public record PendingMaimu(Long groupId, String title, String message, String maimuColor,
                           String writerName, boolean anonymous, Integer sugarContent,
                           LocalDateTime createdAt) {}
```

### 3-3. 반영 경로 — `flush()`

```java
@Scheduled(fixedDelay = 200)   // 이전 실행이 끝난 뒤 200ms 후에 다시
public void flush() {
    List<String> raw;
    do {
        raw = redisTemplate.opsForList().range(BUFFER_KEY, 0, BATCH_SIZE - 1);  // LRANGE 0..999
        if (raw == null || raw.isEmpty()) return;    // 버퍼가 비면 종료
        flushBatch(raw);
    } while (raw.size() == BATCH_SIZE);   // 꽉 찬 배치였다면 뒤에 더 있다는 뜻 → 계속 비운다
}
```

**`fixedDelay`를 쓴 이유:** `fixedRate`(고정 주기)는 이전 실행이 안 끝나도 다음을 시작해 겹칠 수 있다.
`fixedDelay`는 "끝난 뒤 200ms"라 겹치지 않는다. 플러시는 한 번에 하나만 돌아야 LTRIM 위치 계산이 안전하다.

**`do-while`를 쓴 이유:** 트래픽이 몰려 버퍼에 1000건 넘게 쌓였을 때, 200ms를 기다리지 않고
이번 주기에 끝까지 비운다. 1000건을 꽉 채워 꺼냈다면 "뒤에 더 있다"는 신호다.

```java
private void flushBatch(List<String> raw) {
    List<PendingMaimu> items = raw.stream().map(this::fromJson).toList();

    // ① 이 배치에 등장하는 사물함과 주인을 "한 번의 쿼리"로 모두 가져온다 (N+1 방지)
    Map<Long, Group> groups = groupRepository.findAllByIdInWithMember(
                    items.stream().map(PendingMaimu::groupId).distinct().toList())
            .stream().collect(Collectors.toMap(Group::getId, Function.identity()));

    // ② 버퍼에 있는 동안 사물함이 삭제됐으면 넣을 곳이 없다 → 거른다
    List<PendingMaimu> valid = items.stream().filter(p -> groups.containsKey(p.groupId())).toList();

    List<PendingMaimu> inserted;
    try {
        transactionTemplate.executeWithoutResult(status -> insert(valid));  // ③ multi-row INSERT
        redisTemplate.opsForList().trim(BUFFER_KEY, raw.size(), -1);        // ④ 성공 후에만 버퍼에서 제거
        inserted = valid;
    } catch (DataIntegrityViolationException e) {
        // ⑤ 배치 중 1건이라도 제약 위반 → 전체 롤백. 한 건씩 재시도하며 불량만 격리
        inserted = insertOneByOne(raw, groups);
    }
    notifyOwners(inserted, groups);   // ⑥ DB 반영 후에만 알림
}
```

**번호별 "왜":**

- **① N+1 방지** — 1000건 중 사물함이 50종이면, 각각 조회하면 쿼리 50번이다.
  `IN (...)` 한 번으로 묶고 `JOIN FETCH`로 주인까지 한 번에 가져온다. (알림 보낼 때 주인 id가 필요하다.)
- **② 삭제된 사물함 방어** — 쪽지가 버퍼에 있는 동안 주인이 사물함을 지울 수 있다.
  그 쪽지는 넣을 부모가 없으므로(FK 위반) 미리 거른다.
- **③ multi-row INSERT** — `insert()`가 `JdbcTemplate.batchUpdate`를 쓴다(다음 절). 이게 성능의 핵심.
- **④ 순서가 핵심: INSERT(커밋) → 그 다음에 LTRIM.** 버퍼에서 먼저 지우고 INSERT하면,
  INSERT 전에 서버가 죽었을 때 그 쪽지는 영영 사라진다. **"커밋이 성공한 것만 버퍼에서 지운다"** 는
  순서가 **at-least-once(최소 1회 보장)** 를 만든다. (트레이드오프는 8장 참고.)
- **⑤ 불량 1건 격리** — 배치는 "전부 성공 아니면 전부 실패(원자성)"다. 1건이 잘못되면 999건까지 롤백된다.
  그 1건 때문에 버퍼가 영원히 막히면 안 되므로, 실패 시 한 건씩 넣어보고(`insertOneByOne`)
  진짜 불량만 `maimu:write-buffer:dead`(격리 큐)로 빼낸다.
- **⑥ 알림은 DB 반영 뒤에** — 알림 토스트를 눌러 들어갔는데 쪽지가 아직 DB에 없으면 빈 화면이 된다
  (read-your-writes 위반). 그래서 INSERT가 끝난 **뒤에** SSE를 보낸다.

### 3-4. 왜 JPA가 아니라 `JdbcTemplate.batchUpdate`인가 (가장 자주 받을 질문)

```java
private static final String INSERT_SQL = """
        INSERT INTO maimus (group_id, maimu_title, ... , created_at, modified_at)
        VALUES (?, ?, ... , ?, ?)
        """;

private void insert(List<PendingMaimu> items) {
    jdbcTemplate.batchUpdate(INSERT_SQL, items.stream().map(p -> {
        Timestamp at = Timestamp.valueOf(p.createdAt());
        return new Object[]{ p.groupId(), p.title(), ... , at, at };
    }).toList());
}
```

이유를 알아야 면접에서 설명할 수 있다:
- MAIMU의 `Maimu` 엔티티는 PK가 `@GeneratedValue(strategy = IDENTITY)`, 즉 **MySQL AUTO_INCREMENT**다.
- IDENTITY 전략에서는 Hibernate가 INSERT 직후 생성된 id를 곧바로 알아야 해서 **배치 INSERT를 할 수 없다**
  (한 건씩 INSERT하고 매번 id를 받아온다). JPA로 `saveAll`을 해도 내부적으로는 1000번 INSERT다.
- 그래서 영속성 컨텍스트가 필요 없는 대량 쓰기는 **JdbcTemplate으로 직접** 한다.
- 그리고 JDBC URL에 **`rewriteBatchedStatements=true`** 를 켜야, MySQL 드라이버가
  `batchUpdate`의 1000건을 **`INSERT ... VALUES (...),(...),(...)` 한 문장**으로 합쳐 보낸다.
  이 플래그가 없으면 batchUpdate여도 네트워크 왕복이 1000번 일어난다. (`application.yml`, `deploy.yml`에 설정.)

### 3-5. 장애 시 데이터가 안전한가 (내구성)
- 쪽지가 Redis 버퍼에 있는 동안의 안전장치가 **Redis AOF**다. `appendfsync everysec`로,
  Redis가 1초마다 디스크에 기록한다. Redis가 죽어도 최대 1초 분량만 유실 위험이 있다.
- EC2 운영 Redis에 `appendonly yes`가 켜져 있는 것을 직접 확인했다.

---

## 4. [코드] Cache-aside — 읽기 캐시

### 4-1. Cache-aside 패턴이란
"캐시 옆에 두고(aside) 애플리케이션이 직접 관리한다"는 뜻이다. 흐름은 항상 이 3단계다:
1. 캐시를 먼저 본다 → 있으면(**hit**) 그대로 반환, DB 안 감
2. 없으면(**miss**) DB에서 읽는다
3. 읽은 값을 캐시에 넣어두고 반환 (다음 사람은 hit)

### 4-2. 코드 — `GuestGroupCache`

```java
public GuestResponse get(Long groupId, Supplier<GuestResponse> loader) {
    String cached = redisTemplate.opsForValue().get(key(groupId));
    if (cached != null) return read(cached);          // ① hit → DB 안 감
    GuestResponse fresh = loader.get();                // ② miss → DB에서 읽기
    redisTemplate.opsForValue().set(key(groupId), write(fresh), TTL);  // ③ 캐시에 적재 (TTL 60초)
    return fresh;
}
public void evict(Long groupId) { redisTemplate.delete(key(groupId)); }  // 무효화
```

`loader`가 `Supplier`(값을 만드는 함수)인 이유: 캐시 클래스가 "DB를 어떻게 읽는지"를 몰라도 되게 하려고.
호출부가 "미스일 때 뭘 할지"를 람다로 넘긴다. 아래 `GuestService`가 그 람다를 준다:

```java
// GuestService.java
public GuestResponse enterGroup(String token){
    Invitation invitation = verifyInvitation(token);
    return guestGroupCache.get(invitation.getGroupId(),
            () -> loadGroupSummary(invitation.getGroupId()));  // 미스일 때만 실행됨
}

private GuestResponse loadGroupSummary(Long groupId){
    Group group = groupRepository.findById(groupId)            // 락 없음 (개선 전엔 FOR UPDATE였음)
            .orElseThrow(...);
    return GuestResponse.builder()
            .id(group.getId()).groupName(group.getGroupName()).groupColor(group.getGroupColor())
            .maimuCount(maimuRepository.countByGroupId(group.getId()).intValue())  // COUNT만 (전체 로딩 X)
            .build();
}
```

### 4-3. 무효화(언제 캐시를 지우나)
캐시의 영원한 숙제는 "값이 바뀌었는데 캐시가 옛날 값을 들고 있는 것"이다. 세 지점에서 `evict`를 부른다:
- **새 쪽지 DB 반영 시** — `MaimuWriteBackBuffer.notifyOwners`에서 `guestGroupCache.evict(groupId)` (개수가 늘었으니까)
- **쪽지 삭제 시** — `MaimuService.deleteMaimu`
- **사물함 수정/삭제 시** — `GroupService.editGroup`, `deleteGroup`

그리고 혹시 놓친 경우를 대비해 **TTL 60초** 를 둔다. 최악의 경우에도 오차는 60초를 못 넘는다.
(쪽지 개수는 몇 초 늦어도 문제없는 데이터라 이 정도 느슨함을 허용했다.)

### 4-4. 측정 중 발견한 함정 (이 이야기가 면접에서 강력하다)
처음엔 캐시를 붙였는데도 처리량이 기대만큼 안 올랐다. 원인은 서비스 클래스에 붙어 있던
`@Transactional(readOnly = true)` 였다. 이게 있으면 **캐시 hit여서 DB를 안 읽어도, 메서드에 들어가는 순간
트랜잭션이 시작되며 DB 커넥션을 하나 빌려온다.** 캐시 효과가 커넥션 풀(기본 10개)에 묶여버린 것이다.
→ `GuestService`에서 클래스 레벨 트랜잭션을 제거했다. (리포지토리 메서드는 각자 트랜잭션을 갖는다.)

"캐시를 붙였는데 왜 안 빨라졌지? → 프로파일로 보니 커넥션 대기였고 → 원인은 트랜잭션 애너테이션이었다"
이 **문제 해결 과정 자체**가 '내가 직접 측정하고 고쳤다'는 증거다.

### 4-5. 어디에 캐시를 넣고 어디엔 안 넣었나 (판단 근거)
"무지성으로 다 캐싱"이 아니라는 걸 보여주는 표. 캐시는 **읽기가 많고(hit율 높고) + 자주 안 바뀌는** 데이터에만 이득이다.

| 엔드포인트 | 캐시 | 이유 |
| --- | :---: | --- |
| 초대 링크 조회 | O | 같은 키를 수백 명이 읽음, 값은 거의 안 바뀜 |
| 내 사물함 목록(`/group/all`) | X | 사용자 1명만 읽고, 쪽지 올 때마다 개수가 바뀜 → hit율 낮고 무효화만 비쌈 |
| 쪽지 목록 | X | 페이지·읽음·즐겨찾기가 계속 바뀌는 개인 데이터 |
| 쪽지 열기 | X | 조회가 곧 쓰기(읽음 처리)라 캐시 불가 |

---

## 5. [코드] SSE — 실시간 알림

### 5-1. SSE가 뭐고 왜 WebSocket이 아닌가
**SSE(Server-Sent Events)** 는 서버가 클라이언트로 **한 방향**으로 계속 데이터를 밀어보내는 기술이다.
연결을 한 번 열어두면 서버가 원할 때 이벤트를 보낸다. 알림은 "서버 → 사용자" 한 방향이면 충분하므로
양방향인 WebSocket보다 가볍고, 일반 HTTP라 끊겨도 자동 재연결이 쉽다.

### 5-2. 서버 — `SseEmitters`

```java
// 회원 id 하나에 연결 여러 개 (한 사람이 탭을 여러 개 열 수 있으니까)
private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

public SseEmitter subscribe(Long memberId) {
    SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);   // 30분 타임아웃
    emitters.computeIfAbsent(memberId, id -> ConcurrentHashMap.newKeySet()).add(emitter);

    Runnable remove = () -> emitters.computeIfPresent(memberId, (id, set) -> {
        set.remove(emitter);
        return set.isEmpty() ? null : set;    // 마지막 연결이 사라지면 맵에서 회원 키도 제거(메모리 누수 방지)
    });
    emitter.onCompletion(remove);  // 정상 종료 시
    emitter.onTimeout(remove);     // 타임아웃 시
    emitter.onError(e -> remove.run());  // 에러 시

    send(emitter, SseEmitter.event().name("connect").data("connected"));  // 첫 이벤트 즉시 전송
    return emitter;
}

public void send(Long memberId, String eventName, Object data) {
    Set<SseEmitter> targets = emitters.get(memberId);
    if (targets == null) return;   // 접속 중이 아니면 → 아무것도 안 함 (메일 다이제스트가 대신 알림)
    targets.forEach(e -> send(e, SseEmitter.event().name(eventName).data(data, APPLICATION_JSON)));
}

@Scheduled(fixedRate = 30_000)   // 30초마다 하트비트
public void heartbeat() {
    emitters.values().forEach(set -> set.forEach(e -> send(e, SseEmitter.event().comment("ping"))));
}
```

**설명할 포인트:**
- `ConcurrentHashMap` — 여러 요청 스레드가 동시에 구독/해제하므로 동시성 안전한 맵이 필요하다.
- **연결 정리 3종(onCompletion/onTimeout/onError)** — 끊긴 연결을 맵에 계속 두면 메모리 누수 + 죽은 연결에
  계속 쓰기 시도가 일어난다. 세 경우 모두 맵에서 제거한다.
- **첫 이벤트 즉시 전송** — nginx 같은 프록시는 첫 바이트가 올 때까지 응답 헤더를 안 흘려보낼 수 있다.
  `connect` 이벤트를 바로 보내 연결을 확정한다.
- **하트비트 30초** — nginx 기본 유휴 타임아웃이 60초다. 30초마다 주석(`:ping`) 이벤트를 보내 연결을 살려둔다.
- **접속 안 했으면 조용히 끝** — 이게 기존 메일 알림과 역할을 나눈다. **접속 중이면 SSE, 아니면 메일.**

### 5-3. 컨트롤러 + 보안 설정

```java
@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(@AuthenticationPrincipal Long memberId, HttpServletResponse response) {
    response.setHeader("X-Accel-Buffering", "no");  // nginx 응답 버퍼링 끄기 (즉시 전달)
    return sseEmitters.subscribe(memberId);
}
```

`SecurityConfig`에 두 가지를 넣었다:
```java
// ① 알림 경로를 로그인 회원 전용으로
"/v1/api/notification/**"  // USER_URL 목록에 추가
// ② 비동기 디스패치 허용 — 이걸 빠뜨리면 SSE가 깨진다
.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
```
**②의 이유:** SseEmitter는 비동기 요청이다. 연결이 끝나 서블릿이 다시 처리(ASYNC dispatch)할 때
Spring Security 필터를 **다시** 타는데, 이때 인증 정보가 없어 403이 날 수 있다. ASYNC 디스패치를 허용해 막는다.

### 5-4. 프론트 — `MaimuNotifier` (핵심만)
- 브라우저 기본 `EventSource`는 **커스텀 헤더(Authorization)를 못 싣는다.** MAIMU는 토큰을 헤더로 보내므로
  `fetch`로 스트림을 열고 직접 파싱한다(`readEvents.js`). 외부 라이브러리 추가 없음.
- 토큰 만료(`T-003`)면 기존 다른 API와 **같은 방식**으로 재발급 후 재연결한다.
- 알림이 오면 (1) 토스트를 띄우고 (2) `window`에 `maimu:new` 이벤트를 쏜다 →
  `MainPage`가 이걸 듣고 안 읽은 개수를 다시 불러온다. 토스트를 누르면 해당 사물함으로 이동.
- 페이지별 토스트와 안 섞이게 **전용 `ToastContainer`** 를 `createPortal`로 `body`에 붙인다.

---

## 6. [테스트] k6 부하 스크립트의 구조와 의미

부하테스트 파일은 전부 `load-test/`에 있다. 이 장은 "스크립트가 뭘 하는지"와 "왜 이렇게 쟀는지"다.

### 6-1. 두 가지 측정 모델 — 이게 제일 중요하다

| 모델 | k6 executor | 뭘 재나 | 언제 |
| --- | --- | --- | --- |
| **closed (처리량)** | `constant-vus` | 가상 사용자 N명이 쉬지 않고 요청 → **최대 몇 TPS까지 되나** | 처리량(TPS) |
| **open (지연)** | `constant-arrival-rate` | 초당 정확히 N건을 **무조건** 보냄 → 그때 **지연 분포** | 지연(latency) |

**왜 지연은 꼭 open model로 재야 하나 (coordinated omission):**
closed model은 "응답을 받아야 다음 요청을 보낸다." 그래서 서버가 느려지면 **부하 발생기도 자동으로 요청을 덜 보낸다.**
결과적으로 느린 구간이 측정에서 빠져 **지연이 실제보다 좋게** 나온다. 이게 coordinated omission(협조적 누락)이다.
open model은 서버가 느려도 초당 N건을 꼬박꼬박 보내므로, 밀리는 상황이 그대로 숫자에 잡힌다.
→ "지연을 정직하게 쟀다"는 걸 보여주는 포인트.

`common.js`가 이 둘을 환경변수 `MODE`로 전환한다:
```js
if (MODE === 'latency')
  return { executor: 'constant-arrival-rate', rate: RATE, timeUnit: '1s', ... };  // open
return { executor: 'constant-vus', vus: VUS, duration };                           // closed
```

### 6-2. 통계를 p99/p99.9로 본 이유
`summaryTrendStats = ['avg','med','p(90)','p(95)','p(99)','p(99.9)','max']`.
**평균은 꼬리를 숨긴다.** 1000건 중 999건이 5ms고 1건이 3초여도 평균은 8ms로 멀쩡해 보인다.
하지만 그 1건을 겪은 사용자에겐 장애다. 그래서 **p99(상위 1%), p99.9(상위 0.1%)** 를 본다.
증권 시스템에서 꼬리 지연을 중요하게 보는 것과 같은 이유다.

### 6-3. 세 가지 시나리오 파일
- `write-maimu.js` — 쪽지 작성. `TARGET=hot`이면 인기 사물함 1곳, `spread`면 전체에 분산.
- `read-guest.js` — 초대 링크 조회(인기 사물함 1곳).
- `sse-latency.mjs` — 알림 종단 지연(Node 스크립트). k6는 SSE를 기본 지원 안 해서 Node로 쟀다.
  "작성 요청을 보낸 시각 → 주인의 SSE 연결에 이벤트가 도착한 시각" 차이를 300번 재서 분포를 낸다.

### 6-4. 공정한 비교를 위한 장치
- **매 측정 전 DB를 같은 상태로 재시드** (회원 50, 사물함 50, 인기 사물함에 기존 쪽지 300개)
- **10초 워밍업** 후 본 측정 (JVM JIT 컴파일·커넥션 풀이 데워진 뒤에 재야 공정)
- v0/v1/v2를 **완전히 같은 스크립트·같은 부하**로 측정

---

## 7. [재현] 내 손으로 처음부터 다시 측정하는 법

> 목표: 이 절만 따라 하면 누구든 같은 수치를 다시 뽑을 수 있다.
> 필요한 것: Docker, Java 17, Node 18+, k6. (측정 당시 환경: Intel Core Ultra 9 285H 16코어 / RAM 31GB / Windows 11)

### 7-1. MySQL·Redis 띄우기 (벤치 전용, 운영과 분리)
```bash
docker run -d --name bench-mysql -p 13306:3306 -e MYSQL_ROOT_PASSWORD=bench -e MYSQL_DATABASE=maimu \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci \
  --innodb-buffer-pool-size=128M --performance-schema=OFF --max-connections=50
docker run -d --name bench-redis -p 16379:6379 redis:7 --appendonly yes --appendfsync everysec
```
MySQL 옵션을 운영과 똑같이(`buffer-pool 128M`, `max-connections 50`) 맞춘 게 중요하다. 그래야 운영과 비슷한 수치가 나온다.

### 7-2. 스키마 만들기
저장소에 DDL이 없다(`ddl-auto: none`). **베이스라인 jar를 `ddl-auto=create`로 딱 한 번** 띄워 엔티티대로 스키마를 만든다:
```bash
DB_URL='jdbc:mysql://localhost:13306/maimu?...&rewriteBatchedStatements=true' DB_USERNAME=root DB_PASSWORD=bench \
REDIS_HOST=localhost JWT_SECRET_KEY=dummy ... \
java -jar build/libs/chosim-0.0.1-SNAPSHOT.jar --spring.data.redis.port=16379 --spring.jpa.hibernate.ddl-auto=create
# 테이블 3개(members, lockers, maimus)가 생기면 끄고, 이후부터는 ddl-auto 옵션 없이 띄운다
```

### 7-3. 시드 데이터 넣기
`seed.sql`이 회원 50 / 사물함 50(group_id = member_id) / 인기 사물함(1번)에 기존 쪽지 300건을 넣는다.
`WITH RECURSIVE`로 50·300행을 한 번에 만든다. **한글이 깨지지 않게 `--default-character-set=utf8mb4`로 넣는다:**
```bash
docker exec -i bench-mysql mysql --default-character-set=utf8mb4 -uroot -pbench maimu < seed.sql
```

### 7-4. 테스트용 토큰 준비 (인증 우회가 아니라 테스트 픽스처)
- **액세스 토큰**: 부하테스트는 OAuth 로그인을 자동화할 수 없으므로, 로컬 벤치 키로 회원별 JWT를 만들어 쓴다
  (`jwt.js`). 이건 **로컬 벤치 DB 전용**이며 운영과 무관하다.
- **초대 토큰**: 각 사물함에 `POST /group/{id}/invitation`을 호출해 받은 UUID를 `tokens.json`에
  `[{groupId, token}]` 형태로 저장한다. (이 파일은 `.gitignore`에 있어 커밋되지 않는다.)

### 7-5. 측정 실행
```bash
cd load-test
# 처리량 (closed): 인기 사물함 집중 / 전체 분산 / 읽기
MODE=throughput TARGET=hot    VUS=100 DURATION=30s NAME=v2-write-hot    k6 run write-maimu.js
MODE=throughput TARGET=spread VUS=100 DURATION=30s NAME=v2-write-spread k6 run write-maimu.js
MODE=throughput               VUS=100 DURATION=30s NAME=v2-read         k6 run read-guest.js
# 지연 (open): 고정 도착률
MODE=latency TARGET=hot RATE=60  DURATION=30s NAME=v2-write-lat k6 run write-maimu.js
MODE=latency            RATE=100 DURATION=30s NAME=v2-read-lat  k6 run read-guest.js
# 알림 종단 지연
ACCESS_TOKEN=<주인 토큰> SAMPLES=300 node sse-latency.mjs
```
v0·v1 수치를 뽑으려면 각 버전의 jar로 앱만 바꿔 띄우고 같은 명령을 돌리면 된다
(이번 작업에선 버전별 jar를 따로 빌드해 `v0-baseline.jar` / `v1-query-fix.jar` / `v2-redis.jar`로 보관해 측정했다).

### 7-6. 데이터 유실 0 검증 (write-back에서 가장 중요한 검증)
```bash
# 1) 재시드  2) 인기 사물함에 30초 부하  3) 버퍼가 빌 때까지 대기  4) 접수 수 == DB 행수 확인
접수(201 응답 수) == maimus 테이블 증가분  →  유실 0
# 실제 결과: 387,869건 접수 == 387,869건 반영, 버퍼 최대 적체 7,482건, 종료 후 384ms 내 소진
```
k6 요약의 `checks.passes`(201 개수)와 `SELECT COUNT(*)-300`(시드 제외 증가분)을 비교하면 된다.

### 7-7. 결과 읽는 법
- k6가 끝나면 `results/<NAME>.json`에 **전체 요약**이 저장된다(원본 그대로 커밋돼 있다).
- `common.js`의 `summary()`가 콘솔에 핵심 한 줄을 찍는다:
  `TPS=... p50=... p95=... p99=... p99.9=... fail=...`
- `fail`(= `http_req_failed`)이 **0%** 인지 먼저 본다. 에러가 나면서 빠른 건 의미가 없다.

---

## 8. 면접 예상 질문 — 내 답변 스크립트

각 답을 **"내가 측정했다 / 내가 트레이드오프를 안다"** 톤으로.

**Q. 그냥 Redis 붙여서 빨라진 거 아닌가요?**
> 아닙니다. 먼저 락과 전체 로딩만 제거한 버전(v1)을 따로 측정했습니다. 그것만으로 인기 사물함 쓰기가
> 63.9 → 430 TPS로 올랐습니다. 가장 큰 병목은 Redis 유무가 아니라 **불필요한 비관적 락**이었고,
> Redis write-back은 그 위에서 DB 커밋을 요청 경로에서 빼내 추가로 9,251 TPS까지 올린 겁니다.
> 원인을 단계별로 분리해서 측정했기 때문에 이렇게 말할 수 있습니다.

**Q. Write-back인데 Redis가 죽으면 데이터가 날아가지 않나요?**
> 두 겹으로 막았습니다. 첫째, Redis AOF를 `everysec`로 켜서 유실 범위를 최대 1초로 제한했습니다.
> 둘째, 코드에서 **INSERT 커밋이 성공한 다음에만** 버퍼를 지웁니다(LRANGE→INSERT→LTRIM 순서).
> 그래서 at-least-once입니다. 금융 원장처럼 1건도 못 잃는다면 `appendfsync always`나 Kafka 같은
> 복제 로그로 바꾸는 게 맞고, 이건 요구 내구성에 따른 트레이드오프라고 봅니다.

**Q. 그럼 중복 저장은요?**
> INSERT 커밋 직후 LTRIM 직전의 아주 좁은 구간에서 서버가 죽으면, 그 배치가 재기동 때 한 번 더 들어갈 수 있습니다.
> 지금은 at-least-once고, exactly-once가 필요하면 요청마다 UUID를 부여하고 DB 유니크 제약으로 멱등하게 만들면 됩니다.

**Q. TPS 9,251은 DB에 다 쓴 게 아니라 Redis에 넣은 것 아닌가요?**
> 맞습니다. 그래서 **접수 건수 == DB 반영 건수**를 따로 검증했습니다. 30초간 387,869건을 접수했고
> 전부 DB에 반영됐습니다. 부하 중 버퍼 최대 적체는 7,482건(약 0.6초 분량), 부하 종료 후 384ms 만에 다 비워졌습니다.
> 즉 반영이 접수를 따라잡고 있어서 버퍼가 무한히 쌓이지 않는다는 걸 숫자로 확인했습니다.

**Q. 왜 JPA saveAll이 아니라 JdbcTemplate인가요?**
> 엔티티 PK가 IDENTITY(AUTO_INCREMENT)라 Hibernate가 배치 INSERT를 못 합니다. saveAll을 해도 1000번 INSERT입니다.
> 그래서 JdbcTemplate batchUpdate에 `rewriteBatchedStatements=true`를 켜서 1000건을 INSERT 한 문장으로 합쳤습니다.

**Q. 캐시 일관성은 어떻게 맞추나요?**
> 쓰기 반영·삭제·수정 시점에 evict하고, 놓친 경우를 대비해 TTL 60초를 뒀습니다. 쪽지 개수는 몇 초 늦어도
> 되는 데이터라 이 정도 느슨함을 허용했습니다. 그리고 캐시 hit인데도 트랜잭션이 커넥션을 잡던 문제를
> 측정 중에 발견해서 서비스의 클래스 레벨 트랜잭션을 제거했습니다.

**Q. 왜 WebSocket이 아니라 SSE인가요?**
> 알림은 서버→사용자 한 방향이라 양방향인 WebSocket이 과합니다. SSE는 일반 HTTP라 끊겨도 재연결이 쉽고,
> 프록시 호환도 좋습니다. 대신 브라우저 기본 EventSource가 Authorization 헤더를 못 실어서 fetch 스트림으로 직접 구현했습니다.

**Q. 서버를 여러 대로 늘리면요?**
> 지금은 SSE 연결과 플러시 스케줄러가 단일 인스턴스 전제입니다. 확장하면 알림은 Redis Pub/Sub으로
> 브로드캐스트해서 각 서버가 자기 연결에만 보내고, 플러시는 Redis 분산 락(SET NX)으로 리더 1대만 돌게 하면 됩니다.
> 코드에도 이 지점들을 주석으로 표시해 뒀습니다.

**Q. 측정을 믿을 수 있나요?**
> 노트북 한 대에서 부하 발생기와 서버를 같이 돌렸다는 한계는 분명히 있습니다. 그래서 절대값보다
> **같은 조건에서의 상대 비교**로 봐야 합니다. 대신 지연은 coordinated omission을 피하려고 open model로,
> 통계는 평균이 아닌 p99.9까지 봤고, 매 측정 전 DB 재시드와 워밍업으로 조건을 통일했습니다.

---

## 9. 용어 사전

| 용어 | 뜻 |
| --- | --- |
| **Write-back** | 쓰기를 일단 빠른 저장소(Redis)에 하고, 느린 저장소(DB)에는 나중에 모아서 반영하는 전략 |
| **Cache-aside** | 애플리케이션이 캐시를 직접 보고(hit) 없으면 DB에서 읽어 캐시에 채우는(miss) 패턴 |
| **SSE** | Server-Sent Events. 서버가 클라이언트로 한 방향으로 이벤트를 밀어보내는 HTTP 기술 |
| **TPS** | Transactions Per Second. 초당 처리 건수(처리량) |
| **p99 / p99.9** | 느린 순으로 상위 1% / 0.1% 지점의 응답시간. 꼬리 지연을 본다 |
| **coordinated omission** | closed model에서 서버가 느려지면 부하도 줄어 지연이 실제보다 좋게 측정되는 현상 |
| **closed / open model** | 응답을 받아야 다음 요청(closed) vs 초당 고정 건수를 무조건 보냄(open) |
| **at-least-once** | 최소 1회 전달 보장. 유실은 없지만 드물게 중복될 수 있음 |
| **hot key** | 부하가 집중되는 인기 키 하나. MAIMU에선 인기 사물함 |
| **비관적 락 / FOR UPDATE** | 행을 미리 배타적으로 잠가 다른 트랜잭션을 기다리게 하는 방식 |
| **N+1** | 목록 N건을 각각 조회해 쿼리가 N+1번 나가는 비효율. IN·JOIN FETCH로 1번으로 줄임 |
| **fsync** | 디스크에 실제로 기록되도록 강제하는 호출. DB 커밋마다 일어나며 느림 |
| **AOF** | Redis의 Append Only File. 명령을 디스크에 기록해 재시작 시 복구 |
| **TTL** | Time To Live. 캐시 항목의 만료 시간 |

---

### 함께 보기
- 결과·수치 중심 요약: [`PERFORMANCE_REPORT.md`](PERFORMANCE_REPORT.md)
- 부하 스크립트 원본: `load-test/` (common.js, write-maimu.js, read-guest.js, sse-latency.mjs)
- 측정 결과 원본(JSON): `load-test/results/`
- 핵심 코드: `domain/maimu/service/MaimuWriteBackBuffer.java`, `domain/guest/service/GuestGroupCache.java`,
  `common/sse/SseEmitters.java`, `api/notification/controller/NotificationController.java`
