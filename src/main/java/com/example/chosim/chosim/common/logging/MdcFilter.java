package com.example.chosim.chosim.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 traceId 를 만들어 MDC 에 넣는다. 이후 이 요청에서 찍히는 모든 로그 줄에 같은 traceId 가 붙는다.
 *
 * AOP 가 아니라 필터에서 하는 이유: 필터 체인 맨 앞(Spring Security 보다 먼저)에서 돌아야
 * JWT 인증 실패처럼 컨트롤러에 도달하기 전에 끝나는 요청에도 traceId 가 남는다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(TRACE_ID, traceId);
        // 클라이언트가 에러를 신고할 때 이 값을 알려주면 관리자 페이지에서 바로 검색할 수 있다
        response.setHeader(TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            // 톰캣 스레드는 재사용되므로 비우지 않으면 다음 요청에 이전 traceId 가 남는다
            MDC.clear();
        }
    }
}
