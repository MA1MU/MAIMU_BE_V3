package com.example.chosim.chosim.domain.maimu.service;

import com.example.chosim.chosim.api.guest.dto.GuestRequest;
import com.example.chosim.chosim.common.sse.SseEmitters;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.guest.service.GuestGroupCache;
import com.example.chosim.chosim.domain.maimu.enums.MaimuColor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 게스트 마이무 작성의 Write-back 버퍼.
 *
 * 요청 스레드는 Redis List 에 RPUSH 만 하고 바로 응답한다(DB 트랜잭션 없음).
 * 스케줄러가 200ms 마다 최대 1000건씩 꺼내 multi-row INSERT 로 MySQL 에 반영하고,
 * 반영이 끝난 뒤에 사물함 주인에게 SSE 알림을 보낸다(알림을 눌렀을 때 쪽지가 이미 DB 에 있도록).
 *
 * 전달 보장: LRANGE → INSERT(트랜잭션) → LTRIM 순서라 at-least-once.
 * INSERT 커밋 직후 LTRIM 전에 프로세스가 죽으면 그 배치가 다음 기동 때 한 번 더 들어갈 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaimuWriteBackBuffer {

    static final String BUFFER_KEY = "maimu:write-buffer";
    static final String DEAD_LETTER_KEY = "maimu:write-buffer:dead";
    private static final int BATCH_SIZE = 1000;

    // JPA IDENTITY 전략은 Hibernate 배치 INSERT 를 막기 때문에 대량 반영은 JdbcTemplate 으로 한다.
    // URL 에 rewriteBatchedStatements=true 가 있으면 드라이버가 multi-row INSERT 하나로 합친다.
    private static final String INSERT_SQL = """
            INSERT INTO maimus (group_id, maimu_title, maimu_message, maimu_color, maimu_writer_name,
                                is_anonymous, maimu_sugar_content, is_favorite, is_read, is_notified,
                                created_at, modified_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, false, false, false, ?, ?)
            """;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final GroupRepository groupRepository;
    private final SseEmitters sseEmitters;
    private final GuestGroupCache guestGroupCache;

    public record PendingMaimu(Long groupId, String title, String message, String maimuColor, String writerName,
                               boolean anonymous, Integer sugarContent, LocalDateTime createdAt) {
    }

    public void push(Long groupId, GuestRequest request) {
        boolean anonymous = request.isAnonymous();
        PendingMaimu pending = new PendingMaimu(
                groupId,
                request.getTitle(),
                request.getMessage(),
                MaimuColor.fromString(request.getMaimuColor()).name(), // 잘못된 색은 요청 시점에 거른다
                anonymous ? "익명" : request.getWriterName(),
                anonymous,
                request.getSugarContent(),
                LocalDateTime.now()); // 작성 시각은 반영 시각이 아니라 요청 시각
        redisTemplate.opsForList().rightPush(BUFFER_KEY, toJson(pending));
    }

    // ponytail: 단일 인스턴스 전제. 서버를 여러 대로 늘리면 플러시가 중복되므로 Redis 분산 락(SET NX)을 씌운다.
    @Scheduled(fixedDelay = 200)
    public void flush() {
        List<String> raw;
        do {
            raw = redisTemplate.opsForList().range(BUFFER_KEY, 0, BATCH_SIZE - 1);
            if (raw == null || raw.isEmpty()) {
                return;
            }
            flushBatch(raw);
        } while (raw.size() == BATCH_SIZE);
    }

    private void flushBatch(List<String> raw) {
        List<PendingMaimu> items = raw.stream().map(this::fromJson).toList();
        Map<Long, Group> groups = groupRepository.findAllByIdInWithMember(
                        items.stream().map(PendingMaimu::groupId).distinct().toList())
                .stream().collect(Collectors.toMap(Group::getId, Function.identity()));

        // 버퍼에 있는 동안 사물함이 삭제됐으면 넣을 곳이 없다
        List<PendingMaimu> valid = items.stream().filter(p -> groups.containsKey(p.groupId())).toList();
        if (valid.size() < items.size()) {
            log.warn("삭제된 사물함으로 향한 마이무 {}건을 건너뜁니다.", items.size() - valid.size());
        }

        List<PendingMaimu> inserted;
        try {
            transactionTemplate.executeWithoutResult(status -> insert(valid));
            redisTemplate.opsForList().trim(BUFFER_KEY, raw.size(), -1);
            inserted = valid;
        } catch (DataIntegrityViolationException e) {
            // 한 건이라도 제약을 어기면 배치 전체가 롤백된다. 그 한 건 때문에 버퍼가 막히지 않게 한 건씩 넣고 실패분만 격리한다.
            log.error("마이무 일괄 반영 실패, 한 건씩 재시도합니다: {}", e.getMessage());
            inserted = insertOneByOne(raw, groups);
        }

        notifyOwners(inserted, groups);
    }

    private List<PendingMaimu> insertOneByOne(List<String> raw, Map<Long, Group> groups) {
        List<PendingMaimu> inserted = new ArrayList<>();
        for (String json : raw) {
            PendingMaimu p = fromJson(json);
            if (groups.containsKey(p.groupId())) {
                try {
                    insert(List.of(p));
                    inserted.add(p);
                } catch (DataIntegrityViolationException e) {
                    log.error("마이무 반영 불가, 격리 큐로 이동: {}", e.getMessage());
                    redisTemplate.opsForList().rightPush(DEAD_LETTER_KEY, json);
                }
            }
            // 처리한 건만 머리에서 하나씩 뺀다. DB 장애 같은 다른 예외면 여기서 멈추고 나머지는 다음 주기에 재시도된다.
            redisTemplate.opsForList().leftPop(BUFFER_KEY);
        }
        return inserted;
    }

    private void insert(List<PendingMaimu> items) {
        jdbcTemplate.batchUpdate(INSERT_SQL, items.stream().map(p -> {
            Timestamp at = Timestamp.valueOf(p.createdAt());
            return new Object[]{p.groupId(), p.title(), p.message(), p.maimuColor(), p.writerName(),
                    p.anonymous(), p.sugarContent(), at, at};
        }).toList());
    }

    private void notifyOwners(List<PendingMaimu> inserted, Map<Long, Group> groups) {
        inserted.stream()
                .collect(Collectors.groupingBy(PendingMaimu::groupId, Collectors.counting()))
                .forEach((groupId, count) -> {
                    Group group = groups.get(groupId);
                    guestGroupCache.evict(groupId);
                    sseEmitters.send(group.getMember().getId(), "maimu", Map.of(
                            "groupId", groupId,
                            "groupName", group.getGroupName(),
                            "groupColor", group.getGroupColor(),
                            "count", count));
                });
    }

    private String toJson(PendingMaimu pending) {
        try {
            return objectMapper.writeValueAsString(pending);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("마이무 직렬화 실패", e);
        }
    }

    private PendingMaimu fromJson(String json) {
        try {
            return objectMapper.readValue(json, PendingMaimu.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("마이무 역직렬화 실패", e);
        }
    }
}
