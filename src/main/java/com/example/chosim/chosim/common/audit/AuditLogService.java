package com.example.chosim.chosim.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final JdbcTemplate jdbcTemplate;

    // 반드시 다른 빈의 프록시를 통해 호출한다. 커밋 실패도 호출하는 쪽에서 잡는다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, Long actorMemberId, Long targetId, AuditResult result,
                       String errorMessage, String traceId) {
        AuditLog log = AuditLog.builder().action(action).actorMemberId(actorMemberId).targetId(targetId)
                .result(result).errorMessage(errorMessage).traceId(traceId).build();
        // JDBC는 저장 실패 시 Hibernate SqlExceptionHelper의 ERROR 로그를 발생시키지 않는다.
        // 예외 변환은 그대로 받고, 호출 경계에서 WARN으로만 보고한다.
        jdbcTemplate.update("""
                INSERT INTO audit_logs (action, actor_member_id, target_id, result, error_message, trace_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, log.getAction().name(), log.getActorMemberId(), log.getTargetId(), log.getResult().name(),
                log.getErrorMessage(), log.getTraceId(), Timestamp.valueOf(log.getCreatedAt()));
    }
}
