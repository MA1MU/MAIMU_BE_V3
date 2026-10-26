package com.example.chosim.chosim.common.audit;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_created_at", columnList = "created_at,id"),
        @Index(name = "idx_audit_logs_trace_id", columnList = "trace_id")
})
public class AuditLog {
    public static final int ERROR_MESSAGE_LENGTH = 500;
    public static final int TRACE_ID_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditAction action;

    // 연관관계/FK를 두지 않는다. 탈퇴 이후에도 식별자만 남긴다.
    @Column(name = "actor_member_id")
    private Long actorMemberId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditResult result;

    @Column(name = "error_message", length = ERROR_MESSAGE_LENGTH)
    private String errorMessage;

    @Column(name = "trace_id", length = TRACE_ID_LENGTH)
    private String traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AuditLog(AuditAction action, Long actorMemberId, Long targetId, AuditResult result,
                    String errorMessage, String traceId) {
        this.action = action;
        this.actorMemberId = actorMemberId;
        this.targetId = targetId;
        this.result = result;
        this.errorMessage = limit(errorMessage, ERROR_MESSAGE_LENGTH);
        this.traceId = limit(traceId, TRACE_ID_LENGTH);
        this.createdAt = LocalDateTime.now();
    }

    private static String limit(String value, int length) {
        if (value == null) return null;
        // 제어문자는 한 줄로 정리하고, UTF-16 서로게이트 쌍 중간을 자르지 않는다.
        String clean = value.replaceAll("\\p{Cntrl}", " ");
        int count = clean.codePointCount(0, clean.length());
        return count <= length ? clean : clean.substring(0, clean.offsetByCodePoints(0, length));
    }
}
