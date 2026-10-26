# 로깅·모니터링·알림 구현 가이드

> 이 문서 하나로 이번에 바뀐 코드와 기능을 설명할 수 있도록 썼다.
> 도구(Actuator, AOP)는 바뀔 수 있다. 면접에서 남는 것은 세 가지다:
> **① 무엇을 어떤 기준으로 봤나 ② 어떤 상황에 알렸나 ③ 로그 때문에 스레드를 어떻게 나눴나.**

---

## 0. 한눈에 보기

| 기능 | 한 줄 설명 | 핵심 파일 |
| --- | --- | --- |
| 요청 추적 | 요청마다 `traceId`를 만들어 모든 로그 줄에 찍는다 (MDC) | `MdcFilter`, `LoggingAspect` |
| 실행 시간 로그 | 모든 API를 한곳에서 감싸 "누가·무엇을·몇 ms·성공/실패"를 남긴다 (AOP) | `LoggingAspect` |
| 로그 스레드 분리 | 디스크 쓰기를 요청 스레드에서 떼어 전용 스레드로 보낸다 | `logback-spring.xml` |
| 모니터링 | CPU·힙·GC·커넥션 풀·스레드 상태·ERROR 수·큐 길이 수집 | `MonitoringService` |
| 알림 | 7개 기준을 10초마다 검사해 넘으면 메일·화면으로 알린다 | `AlertMonitor` |
| 관리자 페이지 | 위 모든 것을 보는 HTML 한 장 `/admin.html` | `static/admin.html`, `AdminController` |
| 감사 로그 | 세 삭제 API의 성공·실패를 별도 트랜잭션으로 DB에 보관 | `common/audit`, 12절 |

운영 의존성에 추가한 라이브러리는 `spring-boot-starter-actuator` 하나다. 지표 수집용으로만 쓰고 `/actuator/*` URL은 외부에 열지 않았다. 감사 로그의 트랜잭션 검증에는 **테스트 전용 H2**를 추가했다(12절). 운영 jar에는 들어가지 않는다.

---

## 1. 바뀐 파일

| 파일 | 상태 | 역할 |
| --- | --- | --- |
| `common/logging/MdcFilter.java` | 신규 | 요청 시작 시 `traceId`를 MDC에 넣고, 끝나면 비운다 |
| `common/logging/LoggingAspect.java` | 신규 | 컨트롤러 실행 시간 로그, `memberId`를 MDC에 넣는다 |
| `common/config/ThreadPoolConfig.java` | 수정 | 메일 스레드로 MDC를 복사한다 (`TaskDecorator`) |
| `resources/logback-spring.xml` | 신규 | 로그 형식, 파일 롤링, **AsyncAppender(로그 전용 스레드)** |
| `common/monitoring/MonitoringService.java` | 신규 | 로그 파일 tail + 지표 수집 |
| `common/monitoring/AlertMonitor.java` | 신규 | 알림 기준 판정, 메일 발송, 최근 알림 보관 |
| `api/admin/controller/AdminController.java` | 신규 | `GET /v1/api/admin/logs`, `/metrics`, `/alerts` |
| `resources/static/admin.html` | 신규 | 관리자 화면 |
| `common/config/SecurityConfig.java` | 수정 | `/v1/api/admin/**`는 ADMIN만, ERROR 디스패치 허용(9절 버그 수정) |
| `common/config/CorsConfig.java` | 수정 | `https://apimaimu.co.kr` 허용 출처 추가(9.1절 버그 수정) |
| `common/filter/JwtAuthorizationFilter.java` | 수정 | `/admin.html`은 토큰 검사 생략 |
| `common/auth/CustomOauth2SuccessHandler.java` | 수정 | ADMIN 역할도 일반 회원처럼 로그인되게 함 |
| `domain/maimu/service/MaimuWriteBackBuffer.java` | 수정 | Redis 키 상수를 `public`으로 (모니터링에서 재사용) |
| `build.gradle`, `application.yml` | 수정 | Actuator 추가, `/actuator` 비공개, 알림 메일 주소 설정 |
| `.github/workflows/deploy.yml` | 수정 | 로그 볼륨 `maimu-logs`, `ADMIN_EMAIL` 환경변수 |
| `test/.../MonitoringServiceTest.java`, `AlertRuleTest.java` | 신규 | 로그 필터, 알림 판정 단위 테스트 |

---

## 2. 로그 스레드를 어떻게 나눴나 (OS 관점)

### 2.1 문제: 요청 스레드가 디스크를 기다린다

톰캣은 요청 하나를 **스레드 하나**가 끝까지 처리한다(thread-per-request). 이 스레드가 `log.info()` 안에서 파일에 직접 쓰면,

- 파일 쓰기는 **시스템 콜(write)**이다. 커널로 넘어갔다 돌아오고, 버퍼가 차면 디스크 I/O를 기다리며 스레드가 **블로킹**된다.
- 여러 요청 스레드가 파일 하나에 쓰므로 logback 내부에서 **락**을 잡는다. 요청이 몰리면 비즈니스 로직이 아니라 로그 락 앞에서 줄을 선다.

즉 **로그가 응답 시간을 잡아먹는다.**

### 2.2 해결: 생산자-소비자로 분리

```
요청 스레드 N개 (생산자)                        로그 전용 스레드 (소비자)
  log.info() ──┐                                 ┌──> 파일 maimu.log   [AsyncAppender-Worker-ASYNC_FILE]
  log.warn() ──┼──> [ 유한 큐 1024칸 ] ───────────┤
  log.info() ──┘   메모리에 넣고 즉시 리턴         └──> 콘솔            [AsyncAppender-Worker-ASYNC_CONSOLE]
```

`logback-spring.xml`에서 파일·콘솔 출력을 각각 `AsyncAppender`로 감쌌다.

- AsyncAppender는 시작할 때 **워커 스레드 1개를 새로 만든다.** 로컬에서 스레드 덤프(`jcmd <pid> Thread.print`)로 확인한 실제 이름이 `AsyncAppender-Worker-ASYNC_FILE`, `AsyncAppender-Worker-ASYNC_CONSOLE`이다.
- 자바의 플랫폼 스레드는 **OS 스레드와 1:1**이다. 즉 OS 스케줄러가 관리하는 진짜 스레드가 2개 늘었다.
- 둘 다 **daemon 스레드**다. JVM 종료를 막지 않는 보조 스레드라는 뜻이다(종료 시 남은 큐는 최대 1초간 비우고 끝낸다).
- 내부 구조는 `ArrayBlockingQueue` + 워커 스레드다. 큐가 비면 워커는 `take()`에서 **WAITING** 상태로 잠들어 있어 CPU를 쓰지 않는다. 로그가 들어오면 깨어난다.

**왜 소비자를 1개만 두나?** 파일 쓰기는 어차피 순서대로 한 줄씩 해야 한다. 쓰는 스레드가 여러 개면 다시 락 경쟁이 생기고 줄 순서도 섞인다. 소비자 1개면 **락 경합 없이 순차 쓰기**가 된다. 요청 스레드는 큐에 넣는 짧은 순간만 동기화한다.

### 2.3 큐가 꽉 차면? — 이 설계의 핵심 질문

무한 큐는 로그가 폭주하면 메모리가 바닥난다(OOM). 그래서 **유한 큐(1024)**로 두고, 찼을 때의 정책을 정했다.

| 정책 | 결과 |
| --- | --- |
| 생산자를 막는다 | 로그는 안 잃지만 요청이 멈춘다 |
| 버린다 | 요청은 빠르지만 로그를 잃는다 |

선택: **남은 칸이 20% 아래로 떨어지면 INFO 이하는 버리고 WARN/ERROR만 받는다**(`discardingThreshold` 기본값).
완전히 차면 잠깐 막는다(`neverBlock=false`). 장애 때 정말 필요한 에러 로그는 지키면서, 평소 요청 속도도 지키는 절충이다.

- **큐가 차는지는 어떻게 아나?** 관리자 페이지 "비동기 로그 큐" 카드에 보이고, **80% 이상이면 알림**이 간다(3절의 LOG_QUEUE). 80%는 INFO를 버리기 시작하는 선이다. 즉 "로그 유실이 시작됐다"는 알림이다.
- **잃을 수 있는 경우:** 강제 종료(`kill -9`)면 큐에 남은 로그는 사라진다. 비동기의 대가로 받아들였다.

### 2.4 스레드가 바뀌어도 traceId가 유지되는 이유

`traceId`는 MDC에 있고, MDC의 실체는 **`ThreadLocal`(스레드마다 따로 있는 Map)**이다. 로그 스레드는 다른 스레드인데 어떻게 요청의 traceId를 찍을까?
→ `log.info()`를 부르는 순간(아직 요청 스레드일 때) 로그 이벤트 객체 안에 MDC를 **복사해 넣고** 큐에 넣기 때문이다.

같은 문제가 메일 발송에도 있어서 직접 해결했다(`ThreadPoolConfig`).

```java
executor.setTaskDecorator(task -> {
    Map<String, String> context = MDC.getCopyOfContextMap();  // ① 작업을 넘기는 스레드에서 복사
    return () -> {
        if (context != null) MDC.setContextMap(context);       // ② 메일 스레드에 심고
        try { task.run(); }
        finally { MDC.clear(); }                               // ③ 풀 스레드는 재사용되므로 반드시 비운다
    };
});
```

### 2.5 이 프로젝트의 스레드 지도

| 스레드 | 개수 | 하는 일 |
| --- | --- | --- |
| `http-nio-8080-exec-*` | 톰캣 풀(기본 최대 200) | HTTP 요청 처리 |
| `AsyncAppender-Worker-*` | 2 | 로그를 파일·콘솔에 쓰기 (**이번에 추가**) |
| `scheduling-1` | **1** | `@Scheduled` 전부: Write-back 플러시(200ms), 이메일 다이제스트, **알림 검사(10초)** |
| `EmailSender-*` | 코어 수~2배 | 메일 발송 (다이제스트 + **장애 알림**) |

여기서 나온 설계 결정: **알림 메일은 `scheduling-1`에서 보내지 않는다.** 스케줄러 스레드가 1개뿐이라 SMTP 연결(수 초)을 기다리는 동안 Write-back 플러시까지 멈추고, 그러면 마이무가 DB에 반영되지 않는다. 그래서 판정만 스케줄러에서 하고 발송은 `EmailSender` 풀로 넘긴다(`AlertMonitor.sendMail`).

---

## 3. 무엇을 어떤 기준으로 감시하나 + 언제 알리나

### 3.1 알림 규칙 공통 원칙

| 원칙 | 이유 |
| --- | --- |
| **10초마다 검사** | 사람이 반응할 수 있는 단위이면서 검사 비용이 거의 없다(메모리 값 읽기 + Redis LLEN 2번) |
| **N회 연속 넘어야 알림** | GC 직후 CPU 튐처럼 순간적인 값에 울리면 "늑대 소년" 알림이 되어 아무도 안 본다 |
| **같은 알림은 10분 쿨다운** | 장애 중 메일 수백 통이 쌓이면 정작 새 알림을 놓친다 |
| **해소 알림** | 값이 기준 아래로 돌아오면 "정상 복귀"를 남긴다 → 장애가 몇 분 지속됐는지 알 수 있다 |
| **기동 후 30초 유예** | 시작 직후엔 클래스 로딩·JIT 컴파일로 CPU가 튄다 |

이 판정은 `AlertRule.evaluate(value, now)` 하나에 들어 있고, 시간을 인자로 받아서 `AlertRuleTest`에서 연속·쿨다운·해소를 검증한다.

### 3.2 7개 기준

| 기준 | 임계값 | 연속 | 왜 이 값인가 | 넘었다면 의심할 것 |
| --- | --- | --- | --- | --- |
| 프로세스 CPU | 80% | 3회(30초) | 80%를 넘으면 요청이 CPU를 기다리기 시작해 응답 시간이 비선형으로 늘어난다. 순간 튐은 흔하므로 30초 지속만 본다 | 무한 루프, 과도한 직렬화, GC 폭주 |
| 힙 사용률 | 85% | 3회 | 이 선을 계속 넘으면 GC가 자주 돌며 애플리케이션이 멈추고(Stop-The-World), 이어서 OOM 위험 | 메모리 누수, 큰 결과셋 로딩 |
| DB 커넥션 대기 스레드 | 1개 | 3회 | 0이 정상. 기다리는 스레드가 계속 있으면 풀이 부족해 요청이 커넥션을 얻으려고 줄을 서는 중 | 긴 트랜잭션, 락 대기, 느린 쿼리 |
| 10초간 ERROR 로그 | 10건 | 1회 | 평소 ERROR는 드물다. 갑자기 몰리면 사용자가 실패를 겪는 중. 지속을 기다리지 않고 바로 알린다 | 외부 연동 장애, 배포 직후 버그 |
| 비동기 로그 큐 | 80% | 1회 | 2.3절: INFO 로그를 버리기 시작하는 선. "로그 유실 시작" 신호 | 로그 폭증, 디스크 I/O 지연 |
| DB 반영 대기 마이무 | 1000건 | 3회 | Write-back은 200ms마다 최대 1000건을 반영한다. 30초 내내 1000건 이상 쌓여 있으면 유입을 못 따라간다 | DB 장애, 배치 INSERT 실패 |
| 반영 실패 격리 마이무 | 1건 | 1회 | 사용자가 쓴 쪽지가 DB에 못 들어갔다 = **데이터 유실 위험**. 한 건이라도 즉시 알린다 | 데이터 형식 오류, 제약 조건 위반 |

> 임계값은 업계에서 흔히 쓰는 출발점이고 **운영하면서 조정하는 값**이다. `AlertMonitor`의 `rules` 목록 한 곳에서만 바꾸면 된다.

### 3.3 알림 경로

```
scheduling-1 (10초마다)
  readMetrics() → 규칙 7개 evaluate()
     ├─ FIRE    → WARN 로그 "[ALERT] ..." + 최근 알림 목록 + EmailSender 풀에 메일 작업 넘김
     ├─ RECOVER → WARN 로그 "[ALERT 해소] ..." + 최근 알림 목록
     └─ NONE
```

- 메일 수신 주소: 환경변수 `ADMIN_EMAIL`(배포 시 GitHub Secret `ADMIN_EMAIL`). 비어 있으면 로그와 관리자 페이지에만 남는다.
- 알림은 **ERROR가 아니라 WARN으로 찍는다.** ERROR 로그 수가 알림 기준이라 ERROR로 찍으면 알림이 스스로를 부풀리는 되먹임이 생긴다. 메일 발송 실패도 같은 이유로 WARN이다.
- 최근 알림은 메모리에 50건만 둔다. 검사 스레드(쓰기)와 HTTP 스레드(읽기)가 동시에 접근하므로 `check()`와 `status()`를 `synchronized`로 묶었다.

### 3.4 로컬에서 실제로 울려 본 결과

없는 그룹을 10번 조회해서 ERROR 로그 20건을 만들었다(`GlobalExceptionHandler`가 예외 한 번에 ERROR 2줄을 찍는다).

```
22:57:08.543 WARN [scheduling-1] AlertMonitor - [ALERT] ERROR_LOG - 10초간 ERROR 로그 14건 (기준 10건, 1회 연속)
22:57:18.787 WARN [scheduling-1] AlertMonitor - [ALERT 해소] ERROR_LOG - 10초간 ERROR 로그 정상 복귀 (0건)
```

20건이 10초 창 두 개에 14건/6건으로 나뉘어 들어갔고, 첫 창에서 알림, 다음 창에서 해소가 기록됐다.

### 3.5 지표는 어디서 오나

Actuator를 넣으면 Micrometer가 JVM·HikariCP·Logback 지표를 자동으로 `MeterRegistry`에 등록한다. `MonitoringService`는 거기서 값을 꺼내 한 번에 묶을 뿐이다.

| 화면 값 | Micrometer 지표 이름 |
| --- | --- |
| CPU | `process.cpu.usage`, `system.cpu.usage` |
| 힙 | `jvm.memory.used` / `jvm.memory.max` (area=heap) |
| GC | `jvm.gc.pause` (횟수, 누적 시간) |
| 커넥션 풀 | `hikaricp.connections.active` / `idle` / `pending` / `max` |
| 스레드 상태 | `jvm.threads.states` (state=runnable/blocked/waiting/timed-waiting) |
| ERROR 로그 수 | `logback.events` (level=error) |
| 로그 큐 | logback `AsyncAppender`에서 직접 읽음 |
| Write-back | Redis `LLEN maimu:write-buffer`, `maimu:write-buffer:dead` |

**스레드 상태를 읽는 법:** BLOCKED는 `synchronized` 락을 기다리는 중, WAITING/TIMED_WAITING은 큐·풀·sleep에서 대기 중이다.
주의할 점은 **DB 락을 기다리는 스레드가 BLOCKED가 아니라 RUNNABLE로 보인다**는 것이다. JVM 입장에서는 소켓 응답을 읽는 중이기 때문이다. 그래서 DB 병목은 스레드 상태가 아니라 "커넥션 대기 스레드"로 잡는다.

**커넥션 풀 크기 감각(리틀의 법칙 L = λ × W):** 초당 200건이 각각 커넥션을 20ms 잡으면 동시에 필요한 커넥션은 200 × 0.02 = 4개다. 풀 최대는 10개라 여유가 있다. 대기가 생기면 풀을 늘리기 전에 W(트랜잭션·쿼리 시간)부터 줄인다. 이전 성능 개선(`IMPLEMENTATION_GUIDE.md`)의 비관적 락이 바로 W가 길어진 사례다.

---

## 4. 요청 추적: traceId와 MDC

### 4.1 요청 하나가 지나가는 길

```
[MdcFilter]          traceId=6f18de1b 생성 → MDC, 응답 헤더 X-Trace-Id
  ▼
[Spring Security]    JWT → memberId=2
  ▼
[LoggingAspect]      memberId → MDC, 시작 시각
  ▼
[Controller → Service → Repository]   여기서 찍히는 모든 로그에 [6f18de1b] [2]
  ▼
[LoggingAspect]      "[API] GET /v1/api/group/all -> GroupController.getGroupList(..) 35ms"
  ▼
[MdcFilter finally]  MDC.clear()
```

로그 형식: `시각 레벨 [스레드] [traceId] [memberId] 로거 - 메시지`

관리자 페이지에서 traceId로 검색하면 그 요청이 실행한 SQL까지 순서대로 다 나온다. 고객이 에러를 신고하면 응답 헤더의 `X-Trace-Id`만 받으면 된다.

### 4.2 꼭 알아야 할 것

- **`finally`에서 `MDC.clear()`**: 톰캣 스레드는 풀로 돌아가 재사용된다. 안 비우면 다음 요청이 이전 traceId를 물려받는다.
- **traceId는 필터, memberId는 AOP에서 넣는 이유**: AOP는 컨트롤러에 도달한 요청만 감싼다. JWT 실패처럼 필터에서 끝나는 요청도 추적하려면 traceId는 맨 앞(`@Order(HIGHEST_PRECEDENCE)`)에서 만들어야 한다. memberId는 인증이 끝나야 알 수 있다. **값이 생기는 시점이 위치를 정한다.**

### 4.3 실행 시간 로그 (LoggingAspect)

- 컨트롤러 7개에 로그 코드를 복사하지 않고, 모든 요청이 지나는 한 지점에서 처리한다. Spring이 컨트롤러를 **프록시 객체**로 감싸서 앞뒤에 코드를 끼워 넣는 방식이다.
- 1000ms 이상이면 `[API 느림]` WARN, 실패면 `[API 실패]` WARN.
- 예외는 **기록만 하고 다시 던진다** → 기존 에러 응답 형식이 그대로 유지된다.

---

## 5. 관리자 페이지

### 5.1 위치와 화면

`https://apimaimu.co.kr/admin.html`. 백엔드 jar에 들어 있어서 백엔드 배포 때 같이 배포된다(프론트 저장소는 건드리지 않음).

- **서버 상태 카드**(5초 갱신): CPU, 힙+GC, 커넥션 풀, 스레드 상태별 수, 로그 큐, Write-back 대기열, ERROR 누적 수. 막대는 60% 넘으면 주황, 85% 넘으면 빨강.
- **알림 기준표**: 7개 기준의 임계값, 현재 값, 상태(정상 / 초과 N회째 / 알림 중).
- **최근 알림**: 발생·해소 기록.
- **로그**: 키워드·traceId 검색, ERROR만 보기, 줄 클릭 시 그 traceId로 필터, 키워드 검색 시 뒤따르는 스택트레이스 포함.

### 5.2 로그인 방식

1. 관리자가 `maimu.vercel.app`에서 평소대로 소셜 로그인 → 리프레시 토큰 쿠키가 **API 도메인(`apimaimu.co.kr`)**에 저장된다.
2. `admin.html`은 API와 **같은 출처**라 그 쿠키가 자동으로 실린다 → `/v1/api/auth/reissue`로 액세스 토큰을 받는다.
3. 토큰 만료(`T-003`)나 401이면 한 번 재발급 후 재시도한다(프론트 인터셉터와 같은 규칙).
4. 쿠키가 안 통하면 액세스 토큰을 직접 붙여넣는 칸이 있다.

| 호출자 | 관리자 API 결과 (로컬 확인) |
| --- | --- |
| 토큰 없음 | 401 |
| 일반 회원 | 403 |
| 관리자 | 200 |

관리자도 일반 기능을 써야 하므로 회원 API는 `hasAnyRole("MEMBER", "ADMIN")`으로 바꿨다.

### 5.3 관리자 계정 만들기 (운영 DB에서 1회)

```sql
UPDATE members SET member_role = 'ADMIN' WHERE member_id = <내 회원 ID>;
```

그다음 **로그아웃 후 재로그인**해야 한다. 역할은 로그인할 때 토큰 안에 들어가기 때문이다.

### 5.4 보안

- 로그에는 게스트가 쓴 문자열이 섞일 수 있다 → 화면에 그릴 때 `innerHTML`을 쓰지 않고 `textContent`만 쓴다(XSS 방지).
- 로그 API는 파일 경로를 입력받지 않는다(경로 조작 불가). 줄 수는 최대 1000.
- 파일을 통째로 메모리에 올리지 않는다. 한 줄씩 읽으며 마지막 N줄만 `ArrayDeque`에 남긴다.
- `/actuator/*`는 노출하지 않는다(`management.endpoints.web.exposure.exclude: "*"`).

---

## 6. 로그 파일 관리

- `SizeAndTimeBasedRollingPolicy`: 10MB마다 새 파일로 자르고, 7일·합계 100MB까지만 보관한다. 디스크가 로그로 가득 차서 서버가 죽는 사고를 막는다.
- 배포 때마다 컨테이너를 새로 만들므로 로그는 Docker 볼륨 `maimu-logs`(`/logs`)에 둔다. MySQL 데이터를 볼륨에 두는 것과 같은 이유다.
- 관리자 페이지 로그 화면은 이 파일의 마지막 N줄을 읽는다. 그래서 **지금 쓰는 파일만 검색되고**, 잘려 나간 이전 파일은 화면에 안 보인다.

### 6.1 로그를 DB 테이블에 넣지 않은 이유

| 이유 | 설명 |
| --- | --- |
| 서비스 DB가 느려진다 | 요청 하나에 로그가 수십 줄(SQL 로그 포함) 생긴다. 전부 INSERT하면 서비스 쿼리와 같은 커넥션 풀(최대 10개)과 MySQL을 나눠 쓴다. 로그 때문에 본 기능이 느려진다 |
| DB가 죽으면 로그도 못 남긴다 | 로그는 장애 원인을 찾으려고 남긴다. "DB 연결 실패" 에러를 DB에 쓸 수는 없다. 파일은 DB와 무관하게 남는다 |
| 스레드 분리의 의미가 줄어든다 | 디스크 I/O를 요청 경로에서 빼려고 전용 스레드를 만들었다(2절). DB 저장은 네트워크 왕복과 트랜잭션이 더해져 파일 쓰기보다 훨씬 무겁다 |
| 계속 쌓이기만 한다 | 테이블이 끝없이 커져 따로 지우는 작업이 필요하다. 파일은 롤링 정책이 알아서 지운다 |

**DB(또는 다른 저장소)가 맞는 경우**

- **감사 로그**: "관리자가 누구 데이터를 언제 지웠다"처럼 업무·법적으로 남겨야 하고 조건 검색이 필요한 기록. 일반 로그와 성격이 달라 테이블에 두는 게 맞다.
- **오래 보관하고 검색해야 할 때**: 그때도 서비스 DB가 아니라 ELK(Elasticsearch)나 Loki 같은 로그 전용 저장소로 보낸다. 수집기가 파일을 읽어 가는 구조라 지금 구조 위에 그대로 붙일 수 있다.

---

### 6.2 모니터링 값은 실시간인가

**거의 실시간이지만, 지금 이 순간의 값만 보여주고 과거 기록은 남기지 않는다.**

| 항목 | 갱신 주기 | 기록 보관 |
| --- | --- | --- |
| 관리자 페이지 서버 상태 카드 | 5초마다 서버에 다시 물어봄(폴링) | 없음. 화면엔 현재 값만 |
| 알림 검사 | 10초마다 | 최근 알림 50건만 **메모리**에 |
| 로그 화면 | 버튼을 누를 때, 또는 "5초 자동 갱신" 체크 시 | 파일(6절) |

- **값을 읽는 순간의 값이다.** 힙·커넥션 풀·스레드 수·Write-back 대기열은 요청이 들어온 그 시점의 값이다. CPU 사용률은 JVM이 OS에서 받아 오는 "최근 짧은 구간"의 평균이라 순간값에 가깝다.
- **누적값도 있다.** GC 횟수·시간, ERROR 로그 수는 서버가 켜진 뒤부터 쌓인 값이다. 그래서 알림은 ERROR 수 자체가 아니라 "직전 검사 이후 늘어난 만큼"(10초간 증가량)을 본다.
- **재시작하면 초기화된다.** 누적값과 최근 알림 50건은 메모리에 있어서 배포·재시작하면 0부터 다시 시작한다. 알림 기록은 로그 파일의 `[ALERT]` 줄로는 남는다.
- **추이(그래프)는 볼 수 없다.** "30분 전 CPU가 얼마였나"에는 답하지 못한다. 그게 필요해지면 Prometheus가 15초마다 값을 긁어 가 시계열 DB에 저장하고 Grafana로 그래프를 그리는 구조로 간다. Micrometer를 쓰고 있어서 의존성 하나와 설정만 추가하면 된다.
- **왜 폴링(5초)인가?** 서버가 값을 밀어 주는 방식(SSE·WebSocket)도 있지만, 관리자 한 명이 가끔 보는 화면이라 5초 지연은 문제가 없고 구현이 가장 단순하다. 화면을 닫으면 요청도 멈춘다.

---

## 7. 배포

`main`에 push하면 기존 파이프라인 그대로 빌드 → ECR → EC2 재기동된다.

**현재 상태 (2026-10-26 배포 완료)**

| 항목 | 상태 |
| --- | --- |
| 기능 배포 | `ccf0a68` (기능), `613afe4` (CORS 수정, 9.1절) |
| 관리자 계정 | `member_id` 3(카카오), 12(네이버)를 `ADMIN`으로 지정 |
| 알림 메일 | GitHub Secret `ADMIN_EMAIL` 등록, 컨테이너에 주입 확인 |
| 로그 파일 | 운영 컨테이너 `/logs/maimu.log`에 쌓이는 것 확인 |

새 관리자를 추가할 때는 5.3절의 SQL을 실행하고 그 사람이 재로그인하면 된다.
`ADMIN_EMAIL`을 바꾸면 Secret을 고친 뒤 재배포해야 반영된다(환경변수는 컨테이너를 만들 때 들어간다).

---

## 8. 검증한 것 / 못 한 것

**로컬에서 확인함** (벤치용 MySQL·Redis 컨테이너에 앱을 띄움)

- `./gradlew build -x test` 통과. `MonitoringServiceTest`(3), `AlertRuleTest`(3), 기존 `MaimuWriteBackBufferTest` 통과
- 권한 401 / 403 / 200, `/admin.html` 200, `/actuator/health` 비노출, 관리자 토큰으로 회원 API 200
- `X-Trace-Id`로 로그를 검색하면 그 요청의 SQL과 `[API]` 로그가 모두 나옴
- 스레드 덤프에서 `AsyncAppender-Worker-ASYNC_FILE` / `ASYNC_CONSOLE`(daemon) 확인
- ERROR 로그 알림 발생 → 해소 (3.4절)
- 지표 응답 예: `{"processCpuPercent":6.06,"heapUsedMb":126,"heapMaxMb":8064,"gcCount":2,"dbActive":0,"dbIdle":10,"dbWaiting":0,"dbMax":10,"threadStates":{"runnable":9,"timed-waiting":10,"waiting":13},"errorLogCount":1,"logQueueUsed":0,"logQueueCapacity":1024,...}`

**운영에서 확인함** (배포 후 외부에서 호출)

- `/health` 정상, `/admin.html` 200, 관리자 API 토큰 없이 401, `/actuator/*` 비노출
- 모든 응답에 `X-Trace-Id` 헤더가 붙음 → 새 로깅 코드가 운영에서 동작
- 첫 배포에서 관리자 페이지 토큰 재발급이 403으로 막히는 문제를 발견해 고쳤다(9.1절). 로컬에서는 토큰을 직접 넣어 확인해서 이 흐름을 못 잡았다

**확인 못 한 것**

- 알림 **메일 실제 수신**: 아직 운영에서 알림이 울린 적이 없다. 판정과 기록은 로컬에서 확인했다.
- 메일 스레드로 MDC가 넘어가는 것: 다이제스트가 정각에만 돌아 실행 중에 보지 못했다.
- 비동기 로깅 전후 응답 시간 차이: 측정하지 않았다. `load-test/`의 k6로 `AsyncAppender` 적용 전후 p95를 비교하면 숫자로 말할 수 있다.

---

## 9. 작업 중 고친 기존 버그: 403이 302로 바뀌던 문제

**증상:** 권한 없는 API를 부르면 403 대신 **302(로그인 페이지로 리다이렉트)**가 나갔다. 이번 작업 전부터 모든 403 경로가 이랬다.

**원인:** 403 처리기가 `response.sendError(403)` → 서블릿 컨테이너가 `/error`로 **내부 재요청**을 보낸다. 이 재요청에는 인증 정보가 없어서(JWT 필터는 재요청에선 안 돌고, 세션도 STATELESS) `anyRequest().authenticated()`에 걸려 로그인 페이지로 튕겼다.

**수정:** 원 요청은 이미 인가 검사를 통과했으므로 ERROR 디스패치를 허용했다.

```java
.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
```

### 9.1 배포 후 발견한 버그: 관리자 페이지 토큰 재발급이 CORS로 403

**증상:** 운영에 배포하고 네이버로 로그인한 뒤 관리자 페이지를 열자 "Bearer 토큰이 존재하지 않습니다"가 떴다.

**찾은 방법:** 애플리케이션 로그에는 재발급 요청이 아예 없었다. 컨트롤러까지 오지 못했다는 뜻이라 앞단 nginx 접속 기록을 봤고, `POST /v1/api/auth/reissue 403`이 찍혀 있었다.

**원인:** 브라우저는 **같은 출처의 요청이라도 POST에는 `Origin` 헤더를 붙인다**(GET에는 안 붙인다). CORS 허용 목록에는 `https://maimu.vercel.app`만 있어서, `apimaimu.co.kr/admin.html`에서 보낸 재발급 POST를 CorsFilter가 403으로 거부했다. 재발급이 실패하니 토큰 없이 API를 불렀다. 같은 페이지의 GET 요청(`/metrics`)은 Origin이 없어서 CORS를 통과하고 401이 나왔기 때문에 처음엔 인증 문제처럼 보였다.

**수정:** `CorsConfig`에 `https://apimaimu.co.kr`을 허용 출처로 추가했다(`613afe4`).

**교훈:** 로컬 검증은 curl로 토큰을 직접 넣어서 했기 때문에 "브라우저가 쿠키로 재발급받는" 실제 흐름을 거치지 않았다. 사용자가 실제로 지나가는 경로를 그대로 재현하지 않은 검증은 그 경로의 버그를 못 잡는다.

---

## 10. 면접 예상 질문

**Q1. 로그를 왜 별도 스레드로 분리했나? 몇 개를 만들었나?**
파일 쓰기는 시스템 콜이라 디스크를 기다리며 블로킹되고, 여러 요청 스레드가 파일 하나에 쓰면 락 경합이 생긴다. 로그가 응답 시간을 잡아먹지 않도록 생산자-소비자로 분리했다. 요청 스레드는 메모리 큐에 넣고 바로 돌아가고, 출력 대상마다 워커 스레드 1개(파일 1, 콘솔 1)가 순서대로 쓴다. 소비자를 1개로 둔 이유는 순차 쓰기라 락 경합도 줄 순서 섞임도 없기 때문이다.

**Q2. 로그 큐가 꽉 차면?**
무한 큐는 OOM 위험이라 1024칸 유한 큐다. 남은 칸이 20% 아래면 INFO 이하는 버리고 WARN/ERROR만 받는다. 그 시점(80%)에 알림이 가도록 해서 "로그 유실 시작"을 알 수 있게 했다. 완전히 차면 잠깐 막는다. 요청 지연과 로그 유실의 절충이다.

**Q3. 다른 스레드에서 찍은 로그에도 traceId가 나오는 이유는?**
MDC는 ThreadLocal이라 스레드끼리 공유되지 않는다. 비동기 로깅은 로그 이벤트를 만들 때 MDC를 이벤트 안에 복사한다. 메일 스레드 풀에는 TaskDecorator로 직접 복사했다. 풀 스레드는 재사용되므로 작업이 끝나면 반드시 비운다.

**Q4. 알림 기준은 어떻게 정했나? 오탐은 어떻게 막았나?**
"사용자가 곧 느끼는 선"을 기준으로 7개를 정했다. CPU 80%, 힙 85%, 커넥션 대기 1개 이상, 10초간 ERROR 10건, 로그 큐 80%, 반영 대기 1000건, 반영 실패 1건. 오탐을 막으려고 CPU·힙·풀처럼 순간적으로 튀는 지표는 3회(30초) 연속일 때만 울린다. 반대로 데이터 유실(격리 1건)이나 에러 급증은 즉시 울린다. 같은 알림은 10분 쿨다운을 두고, 복귀하면 해소를 남겨 지속 시간을 알 수 있게 했다.

**Q5. 알림 메일을 왜 스케줄러 스레드에서 보내지 않았나?**
Spring 기본 스케줄러는 스레드가 1개고 Write-back 플러시(200ms)도 같이 쓴다. SMTP를 몇 초 기다리면 그동안 쪽지가 DB에 반영되지 않는다. 판정은 스케줄러에서, 발송은 메일 스레드 풀에서 한다.

**Q6. 알림을 ERROR가 아니라 WARN으로 찍은 이유는?**
ERROR 로그 수가 알림 기준이라, 알림을 ERROR로 찍으면 알림이 다음 알림을 부르는 되먹임이 생긴다.

**Q7. 커넥션 풀 대기가 생기면 풀을 늘리면 되나?**
먼저 리틀의 법칙으로 본다. 필요한 커넥션 = 초당 요청 × 커넥션 점유 시간. 대기가 생기면 보통 점유 시간(긴 트랜잭션, 락 대기)이 늘어난 것이므로 그걸 먼저 줄인다. 풀은 DB의 최대 연결 수(50) 안에서만 늘릴 수 있다.

**Q8. DB 락을 기다리는 스레드는 스레드 상태로 어떻게 보이나?**
RUNNABLE이다. JVM 입장에서는 소켓 응답을 읽는 중이다. BLOCKED는 JVM 안의 `synchronized` 경합뿐이다. 그래서 DB 병목은 커넥션 대기 수로 감시한다.

**Q9. traceId는 왜 필터에서, memberId는 왜 AOP에서?**
AOP는 컨트롤러에 도달한 요청만 감싼다. 인증 실패 요청도 추적하려면 traceId는 맨 앞 필터에서 만들어야 한다. memberId는 인증이 끝나야 알 수 있다.

**Q10. Prometheus/Grafana를 안 쓴 이유는?**
서버 1대, 관리자 1명 규모라 수집 서버를 따로 운영할 이유가 없다. 지표는 Micrometer로 이미 표준화돼 있어서, 서버가 늘거나 과거 추이를 봐야 하면 레지스트리만 Prometheus로 바꿔 붙이면 된다.

**Q11. 로그를 왜 DB에 저장하지 않았나?**
로그 INSERT가 서비스 쿼리와 커넥션 풀·DB를 나눠 써서 본 기능이 느려지고, DB 장애 때는 정작 그 장애 로그를 남길 수 없다. 로그 스레드를 분리해 I/O를 요청 경로에서 뺀 의미도 줄어든다. 그래서 파일에 쓰고 롤링으로 용량을 관리한다. 감사 로그처럼 업무상 조건 검색이 필요한 기록만 테이블에 두고, 장기 검색이 필요해지면 ELK·Loki 같은 전용 저장소로 보낸다(6.1절).

**Q12. 모니터링 값은 실시간인가?**
화면은 5초, 알림은 10초 주기로 읽는 거의 실시간 값이다. 다만 현재 값만 보여주고 과거 기록은 저장하지 않아서 추이는 볼 수 없고, 누적값과 최근 알림은 재시작하면 초기화된다. 추이가 필요하면 Micrometer 지표를 Prometheus가 주기적으로 수집해 시계열 DB에 쌓는 구조로 확장한다(6.2절).

**Q13. 업무가 롤백됐는데 감사 로그는 어떻게 남나? 자기 호출은 왜 안 되나?**
세 삭제 컨트롤러를 `AuditAspect`로 감쌌다. 내부 업무 서비스의 트랜잭션 프록시가 롤백을 끝내고 예외를 던지면, AOP가 별도 빈 `AuditLogService.record()`를 호출한다. 이 메서드가 `REQUIRES_NEW`로 자기 트랜잭션을 커밋한 뒤 원래 업무 예외를 다시 던진다. 같은 빈에서 `this.record()`를 부르면 프록시를 안 지나 전파 옵션이 적용되지 않으므로 저장 빈을 분리했다. 성공도 업무 커밋 이후에만 기록해서 커밋 단계의 실패를 SUCCESS로 남기지 않는다.

**Q14. REQUIRES_NEW면 커넥션이 두 개 필요한가? 왜 afterCommit을 안 썼나?**
진행 중인 트랜잭션 안에서 부르면 바깥 커넥션을 붙잡은 채 새 커넥션이 필요하다. 풀이 기본 10개이므로 10개 요청이 모두 바깥 커넥션을 점유하면 감사용 커넥션을 기다리다 풀 타임아웃이 날 수 있다. 이번 API 경계는 업무 프록시가 종료된 **뒤** 호출하므로 일반 경로에서는 업무 커넥션을 반환한 뒤 감사 커넥션을 쓴다. `afterCommit`은 성공만 다루고 실패 기록을 따로 만들어야 하며 콜백 시점에는 업무 리소스가 아직 정리되지 않았을 수 있어, 성공·실패를 같은 경계에서 처리하는 방식을 골랐다.

**Q15. 회원을 삭제하면서 감사 테이블에 회원 FK를 걸면 안 되나?**
같은 회원 행을 바깥 트랜잭션이 비관적 락으로 잡은 동안 감사 INSERT가 FK 검사를 하면, 새 트랜잭션이 바깥 트랜잭션의 락 해제를 기다릴 수 있다. 바깥 호출도 감사 완료를 기다리면 서로 진행하지 못한다. 탈퇴 이후에도 이력을 남겨야 하므로 actor/target은 단순 BIGINT로만 보관하고 FK·JPA 연관관계를 두지 않았다. 현재 API 경계에서는 업무 락 해제 뒤 기록하지만, 추후 호출 위치가 바뀌더라도 FK 경합을 만들지 않는 모델이다.

**Q16. 감사 저장이 실패해도 성공 응답을 주면 이력이 유실되는 것 아닌가?**
맞다. 이번 범위는 **감사 저장 실패가 업무 결과를 뒤집지 않는 최선 노력(best-effort) 기록**이다. 저장·커밋 예외는 프록시 바깥에서 잡아 WARN으로 남기며, DB 장애나 프로세스 강제 종료 시 이력 유실을 완전히 막지는 못한다. 이력 무손실이 필수라면 별도 내구성 큐·저장소와 재처리 정책이 필요하다. `REQUIRES_NEW`가 보장하는 것은 트랜잭션 독립성이지 장애 시 기록 성공 자체가 아니다.

---

## 11. 일부러 안 한 것

| 안 한 것 | 필요해지는 시점 |
| --- | --- |
| Prometheus + Grafana (추이 그래프, 장기 보관) | 서버가 여러 대이거나 "어제 몇 시에 무슨 일이"를 봐야 할 때 |
| ELK/Loki 로그 수집 | 지금은 **현재 로그 파일(최대 10MB)만** 검색된다. 지난 파일까지 검색해야 할 때 |
| Slack 등 메신저 알림 | 메일로 늦게 보는 문제가 생길 때 (`sendMail` 한 곳만 바꾸면 된다) |
| 알림 기준을 설정 파일로 분리 | 운영 중 자주 바꾸게 될 때. 지금은 `AlertMonitor.rules` 한 곳 |
| 응답 시간 p95 알림 | 지금은 1초 넘는 요청을 `[API 느림]` WARN으로만 남긴다 |

---

## 12. 감사 로그 — 업무 결과와 별도로 남기는 삭제 이력

### 12.1 일반 로그와 저장소를 다르게 고른 이유

6.1절의 "로그를 DB에 넣지 않는다"는 원칙은 일반 실행 로그에 관한 것이다. 감사 로그는 세 삭제 행위만 한 건씩 남기므로 목적과 양이 다르다.

| 구분 | 일반 로그 | 감사 로그 |
| --- | --- | --- |
| 질문 | 어느 SQL·코드에서 왜 느리거나 실패했나? | 누가 어떤 대상을 삭제하려 했고 결과가 무엇인가? |
| 양 | 요청·SQL마다 여러 줄 | 지정한 삭제 API 호출당 한 행 |
| 저장 | AsyncAppender → 파일, 7일/100MB 롤링 | `audit_logs` 테이블, 독립 트랜잭션 |
| 조회 | 파일 tail, traceId 문자열 검색 | 관리자 API 최신순 페이지 조회, traceId 인덱스 |
| 업무 롤백 | 파일 로그는 별개로 남음 | 별도 커밋이므로 업무 롤백과 분리됨 |
| 장애 한계 | 강제 종료 시 큐의 로그 유실 가능 | DB/풀 장애·프로세스 종료 시 저장 실패·미기록 가능 |
| 보관 | 자동 롤링 | 자동 삭제 정책은 이번 범위에 없음. 운영 보관 기간·용량 관리가 필요 |

토큰·쪽지 본문·회원 이름·이메일은 별도 컬럼으로 수집하지 않는다. 식별자·행위·결과와 제한된 예외 메시지만 보관한다. 실패 사유는 예외 클래스와 메시지를 합쳐 최대 500자로 제한하고 제어문자를 공백으로 바꾼다. 예외 메시지에 내부 정보가 포함될 수 있으므로 조회는 ADMIN에게만 허용한다.

### 12.2 범위와 실제 실행 순서

| API | action | target_id |
| --- | --- | --- |
| `DELETE /v1/api/maimu/{maimuId}/delete` | `MAIMU_DELETE` | 쪽지 ID |
| `DELETE /v1/api/group/{groupId}` | `GROUP_DELETE` | 그룹 ID |
| `DELETE /v1/api/member` | `MEMBER_WITHDRAW` | 탈퇴 회원 ID |

`@Audited`는 이 세 **컨트롤러 메서드**에만 붙인다. 모두 첫 인자가 대상 ID다. actor는 URL이나 요청 바디가 아니라 `SecurityContextHolder`의 `Long` principal에서, traceId는 `MDC.get(MdcFilter.TRACE_ID)`에서 호출 전에 캡처한다. 회원 삭제 후에도 숫자 actor를 그대로 남길 수 있다.

```text
HTTP → AuditAspect (업무 트랜잭션 바깥)
        → Controller → 업무 Service 프록시
                         BEGIN → 삭제 → COMMIT 또는 ROLLBACK → 리소스 반환
        ← 반환 또는 예외 (커밋 단계의 실패도 여기로 돌아온다)
        → 별도 빈 AuditLogService 프록시
             REQUIRES_NEW → INSERT audit_logs → COMMIT
        → 원래 응답 반환 / 원래 예외 다시 던짐
```

서비스 안의 catch에서 실패를 기록하는 방식은 고르지 않았다. rollback-only 상태뿐 아니라 **메서드 본문 반환 뒤 커밋 중 발생한 오류**까지 구분하려고 업무 프록시 전체를 바깥에서 감싼다. 감사 저장 실패는 별도로 잡으므로 업무 성공을 실패로 바꾸거나 원래 업무 예외를 덮지 않는다.

이것은 HTTP 삭제 시도 기록이다. 인증·인가 필터에서 거절된 요청, 경로 인자 변환 실패, 서비스 직접 호출, 그룹/회원 삭제에 따른 하위 쪽지의 개별 삭제는 별도 감사 행으로 남기지 않는다. 기존 필터/파일 로그 범위다. 컨트롤러에 `@Transactional`을 새로 붙이거나 다른 트랜잭션에서 호출하면 이 경계 가정이 달라지므로 설계를 다시 검토해야 한다.

### 12.3 REQUIRES_NEW의 일반 동작과 커넥션 비용

업무 트랜잭션 **안에서** 별도 빈을 호출하는 일반적인 경우는 다음과 같다. 통합 테스트에서 이 경로도 검증했다.

```text
업무 Tx A / 커넥션 1: BEGIN → 행 잠금·업무 변경
                     │
                     ├─ 일시정지 (커넥션 1과 락은 계속 보유)
                     │     감사 Tx B / 새 커넥션 2: BEGIN → INSERT → COMMIT
                     ├─ 재개
                     └─ 업무 예외 → ROLLBACK
결과: 업무 변경은 취소, 감사 Tx B의 기록은 유지
```

HikariCP 최대는 별도 설정이 없어 기본 **10개**다. 위처럼 중첩하면 요청당 두 커넥션을 동시에 요구하고, 요청이 몰릴 때 풀 고갈·서로 기다리는 상황이 생길 수 있다. `REQUIRES_NEW`가 자동으로 별도 풀을 만들어 주지는 않는다. 이번 실제 API 경로는 12.2절처럼 **업무 Tx 종료 뒤** 기록해 이 동시 점유를 피한다. 그래도 감사 INSERT/커밋 왕복은 응답 시간에 추가되며, DB 장애 시 커넥션 타임아웃을 기다릴 수 있다. 예외 격리는 지연 격리와 다르다.

| 대안 | 장점 | 이번 선택과의 비교 |
| --- | --- | --- |
| 업무 트랜잭션 안에서 `REQUIRES_NEW` | 실패 직전에도 독립 저장 가능 | 추가 커넥션·업무 락 보유 중 저장. 성공을 미리 기록하면 나중 커밋 실패와 어긋남 |
| `afterCommit`에서 성공 기록 | 실제 커밋 이후라는 사실이 명확 | 실패 경로는 별도 구현해야 함. 콜백 시점 업무 리소스가 아직 남을 수 있어 새 트랜잭션과 커넥션 비용 고려 필요 |
| **컨트롤러 AOP → 업무 프록시 종료 → `REQUIRES_NEW`** | 성공·실패·커밋 예외를 같은 경계에서 관찰, 평소 동시 커넥션 점유 감소 | 이번 선택. 프로세스 종료로 업무 커밋과 감사 저장 사이 기록이 빠질 수 있음 |

기존 `MemberService.deleteMember()`의 `REPEATABLE_READ`와 회원/그룹 비관적 잠금은 유지했다. 감사 테이블의 actor/target에는 FK·JPA 연관관계가 없고, 저장 시 회원·그룹을 다시 조회하지 않는다. 따라서 감사 INSERT가 삭제 대상 행의 FK 검사 락을 기다리는 경로가 없다. 다른 공통 DB 자원 경합까지 없다는 뜻은 아니다.

### 12.4 저장 실패와 조회 화면

- `AuditLogService.record()`는 **별도 Spring 빈**이고 `@Transactional(REQUIRES_NEW)`다. 자기 호출하지 않는다.
- `AuditAspect`는 저장 메서드 호출뿐 아니라 **그 프록시의 커밋 실패**도 catch하고 WARN 한 줄만 남긴다. 예외 원문은 파일 로그에 다시 출력하지 않는다.
- INSERT는 JDBC로 실행한다. JPA 저장 실패 시 Hibernate가 자체적으로 ERROR를 출력하는 경로를 피하려는 선택이다. 감사 저장 코드가 ERROR를 직접 찍으면 `AlertMonitor`의 ERROR 증가량을 다시 올릴 수 있다.
- `GET /v1/api/admin/audit-logs?page=0`: 기존 ADMIN 전용 경로 아래, 페이지 크기 20 고정. `createdAt DESC, id DESC`로 같은 시각에도 순서를 확정한다. 음수 page는 기존 `INVALID_INPUT`으로 거절한다.
- 응답은 `content`, `page`, `size`, `totalElements`, `totalPages`를 가진 DTO다. traceId를 누르면 기존 파일 로그 검색으로 연결된다. 과거 파일은 6절의 검색 한계를 그대로 가진다.
- `admin.html`은 셀·실패 사유·traceId 모두 `textContent`로 표시한다. 감사 조회만 실패하면 해당 섹션에 오류를 보이고 기존 지표/로그 화면은 계속 쓴다.
- 세 삭제 서비스에 소유자 검증을 추가했다. 남의 데이터 삭제 시도는 `403`, 감사 결과는 FAILURE다. 관리자도 이 일반 삭제 API로는 본인 소유만 삭제한다. 별도의 관리자 강제 삭제 기능은 만들지 않았다.

### 12.5 스키마 반영 — 사람이 실행해야 함

앱은 `ddl-auto: none`이라 테이블을 만들지 않는다. **이번 작업에서는 운영 서버·DB에 접속하지 않았고 DDL도 실행하지 않았다.**

1. DB 담당자가 대상 환경·백업/보관 정책을 확인한다.
2. 승인된 DB 접속 환경에서 MySQL 클라이언트로 접속한다. 비밀번호는 명령행에 넣지 말고 프롬프트/기존 비밀 관리 경로를 쓴다.
3. 대상 DB를 선택하고 `docs/sql/audit_logs.sql`을 실행한다. 저장소 루트 기준 예시는 아래와 같다.

```text
mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME>
mysql> SOURCE docs/sql/audit_logs.sql;
mysql> SHOW CREATE TABLE audit_logs\G
mysql> SHOW INDEX FROM audit_logs;
```

DDL은 새 테이블 생성용이며 기존 테이블을 변경하거나 삭제하지 않는다. 이미 테이블이 있으면 덮어쓰지 말고 정의를 비교한다. actor/target FK가 없는지와 created_at·trace_id 인덱스를 확인한 뒤 `feat/audit-log`를 main에 머지·push해 배포한다. 배포 후 본인 소유의 테스트 데이터로 세 API의 SUCCESS/FAILURE와 관리자 조회를 점검한다. 테이블이 없는 상태로 먼저 배포되면 업무는 계속되지만 감사 행이 저장되지 않고 WARN이 남는다. 그 사이 이력은 자동 복구되지 않는다.

### 12.6 검증한 것 / 못 한 것

**로컬에서 확인한 것**

- `gradlew.bat build -x test` 통과.
- `AuditTransactionTest` 7개: 실제 Spring AOP·JPA 트랜잭션 + H2(MySQL 모드), 외부 MySQL/Redis와 전체 앱 기동 없음.
  - 업무 DELETE 롤백 후 원래 행 복원 + FAILURE 행 유지, actor/target/traceId 보존.
  - 커밋 직전 예외도 SUCCESS가 아닌 FAILURE로 기록.
  - 정상 삭제는 업무 커밋 후 SUCCESS로 기록하고 실패 사유는 NULL.
  - 감사 테이블을 실제 DROP해도 성공한 업무 커밋 유지, 실패한 업무의 원래 예외 유지.
  - 바깥 Tx가 행 잠금을 가진 채 별도 빈을 호출한 뒤 롤백해도 감사 행은 유지, FK 없음 확인.
  - 배포용 DDL을 H2 MySQL 모드에서 실행, JPA 매핑으로 조회·동일 시각의 최신순 페이지 경계 확인.
- Mockito 단위 테스트: 감사 저장 예외는 WARN만 출력하고 응답 보존, actor/trace 전달, 메시지 길이·제어문자 처리, 세 서비스의 타인 삭제 방어, 관리자 조회 DTO·음수 페이지 거절.
- 최종 선택 실행: 신규 15개 + 기존 `MonitoringServiceTest` 3개 / `AlertRuleTest` 3개 / `MaimuWriteBackBufferTest` 2개 = **23개 모두 통과**. 외부 DB/Redis가 필요한 전체 컨텍스트 테스트는 실행하지 않았다.
- 관리자 HTML의 스크립트를 JSDOM에서 실행해 사용자 문자열이 태그로 해석되지 않는 것, 이전/다음 버튼 상태, traceId 클릭 시 파일 로그 검색 연계를 확인했다. 실제 브라우저·운영 API를 호출한 검증은 아니다.

Mockito만으로는 "새 트랜잭션에서 실제 커밋됐다"를 증명할 수 없어 H2 통합 테스트를 추가했다. 테스트 클래스 자체에는 `@Transactional`을 붙이지 않아 테스트 프레임워크의 자동 롤백에 가리지 않도록 했다. H2는 `testRuntimeOnly`라 운영 의존성이 아니다.

```text
gradlew.bat test --tests "*AuditTransactionTest" --tests "*AuditAspectTest" --tests "*DeleteOwnershipTest" --tests "*AdminAuditControllerTest"
```

**확인하지 않은 것**

- 운영 MySQL DDL 실행, 실제 회원/그룹/쪽지 삭제를 통한 운영 이력 확인, 실제 브라우저 관리자 화면 확인.
- MySQL InnoDB의 `REPEATABLE_READ`·갭 락·동시 탈퇴 상황, Hikari 최대 10개에서의 부하/풀 고갈 재현. H2의 행 잠금 테스트를 MySQL과 동등한 검증으로 주장하지 않는다.
- 감사 추가 전후의 지연/TPS 수치, 프로세스 강제 종료 시 이력 유실 범위, DB 장애 시 최대 응답 지연.
- 새 URL의 인증/인가를 전체 보안 필터 체인으로 호출한 테스트. 기존 `/v1/api/admin/**` ADMIN 규칙을 코드로 확인했고 MockMvc 테스트는 DTO/페이지 계약만 검증한다.

### 12.7 이번 변경 파일

Java 경로는 `src/main/java/com/example/chosim/chosim/`, 테스트 경로는 `src/test/java/com/example/chosim/chosim/` 기준이다.

| 위치 | 파일 |
| --- | --- |
| `common/audit/` 신규 | `AuditAction.java`, `AuditResult.java`, `Audited.java`, `AuditLog.java`, `AuditLogRepository.java`, `AuditLogService.java`, `AuditAspect.java`, `AuditLogQueryService.java` |
| `common/auth/` 신규 | `CurrentMember.java` |
| `common/error/` 수정 | `ErrorCode.java` (`A-001`, 기존 코드와 중복 없음) |
| 관리자 API | `api/admin/controller/AdminController.java`, `api/admin/dto/AuditLogResponse.java` |
| 감사 대상 컨트롤러 | `api/maimu/controller/MaimuController.java`, `api/group/controller/GroupController.java`, `api/member/controller/MemberController.java` |
| 소유자 검증 서비스 | `domain/maimu/service/MaimuService.java`, `domain/group/service/GroupService.java`, `domain/auth/service/MemberService.java` |
| 관리자 화면 | `src/main/resources/static/admin.html` |
| 스키마·문서·테스트 의존성 | `docs/sql/audit_logs.sql`, `docs/OBSERVABILITY_GUIDE.md`, `CLAUDE.md`, `build.gradle` |
| 신규 테스트 | `common/audit/AuditTransactionTest.java`, `common/audit/AuditAspectTest.java`, `common/audit/DeleteOwnershipTest.java`, `api/admin/controller/AdminAuditControllerTest.java` |
