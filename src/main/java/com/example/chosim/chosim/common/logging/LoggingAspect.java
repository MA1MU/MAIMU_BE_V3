package com.example.chosim.chosim.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 모든 컨트롤러 호출을 감싸서 "누가, 어떤 API 를, 얼마나 걸려서, 성공/실패" 를 한 줄로 남긴다.
 * 컨트롤러마다 로그 코드를 넣지 않고 한곳에서 처리하는 것이 AOP 를 쓰는 이유다.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final String MEMBER_ID = "memberId";
    private static final long SLOW_MS = 1000;

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        // 이 시점엔 JWT 필터가 인증을 끝냈으므로 회원 ID 를 알 수 있다 → 서비스 계층 로그에도 찍히도록 MDC 에 넣는다
        Long memberId = currentMemberId();
        if (memberId != null) {
            MDC.put(MEMBER_ID, String.valueOf(memberId));
        }
        String api = requestLine() + " -> " + joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= SLOW_MS) {
                log.warn("[API 느림] {} {}ms", api, elapsed);
            } else {
                log.info("[API] {} {}ms", api, elapsed);
            }
            return result;
        } catch (Throwable e) {
            log.warn("[API 실패] {} {}ms - {}: {}", api, System.currentTimeMillis() - start,
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } finally {
            MDC.remove(MEMBER_ID);
        }
    }

    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // JwtAuthorizationFilter 가 principal 을 Long memberId 로 넣는다. 게스트 요청은 "anonymousUser" 문자열이다
        return (auth != null && auth.getPrincipal() instanceof Long id) ? id : null;
    }

    private String requestLine() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getMethod() + " " + request.getRequestURI();
        }
        return "";
    }
}
