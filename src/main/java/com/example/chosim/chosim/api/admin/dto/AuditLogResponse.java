package com.example.chosim.chosim.api.admin.dto;

import com.example.chosim.chosim.common.audit.AuditAction;
import com.example.chosim.chosim.common.audit.AuditLog;
import com.example.chosim.chosim.common.audit.AuditResult;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, AuditAction action, Long actorMemberId, Long targetId,
                               AuditResult result, String errorMessage, String traceId, LocalDateTime createdAt) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getAction(), log.getActorMemberId(), log.getTargetId(),
                log.getResult(), log.getErrorMessage(), log.getTraceId(), log.getCreatedAt());
    }
}
