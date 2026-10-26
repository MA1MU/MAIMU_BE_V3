package com.example.chosim.chosim.common.audit;

import com.example.chosim.chosim.common.logging.MdcFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 트랜잭션이 없는 컨트롤러에서 업무 서비스 프록시의 커밋/롤백까지 감싼다. */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogService auditLogService;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long actor = auth != null && auth.getPrincipal() instanceof Long id ? id : null;
        Long target = (Long) joinPoint.getArgs()[0];
        String traceId = MDC.get(MdcFilter.TRACE_ID);
        Object response;
        try {
            response = joinPoint.proceed();
        } catch (Throwable failure) {
            recordSafely(audited.value(), actor, target, AuditResult.FAILURE,
                    failure.getClass().getSimpleName() + ": " + failure.getMessage(), traceId);
            throw failure;
        }
        recordSafely(audited.value(), actor, target, AuditResult.SUCCESS, null, traceId);
        return response;
    }

    private void recordSafely(AuditAction action, Long actor, Long target, AuditResult result,
                              String errorMessage, String traceId) {
        try {
            auditLogService.record(action, actor, target, result, errorMessage, traceId);
        } catch (RuntimeException failure) {
            // SQL/사용자 입력이 담길 수 있는 예외 원문은 출력하지 않는다. ERROR 알림 되먹임도 피한다.
            log.warn("감사 로그 저장 실패: action={}, actorMemberId={}, targetId={}, traceId={}, cause={}",
                    action, actor, target, traceId, failure.getClass().getSimpleName());
        }
    }
}
