package com.example.chosim.chosim.common.filter;

import com.example.chosim.chosim.common.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final String SWAGGER_PATH = "/swagger-ui";
    private static final String SWAGGER_PATH_3 = "/v3/api-docs";
    private static final String SWAGGER_FAVICON = "/favicon.ico";
    private static final String HEALTH_CHECK_URL = "/health";
    private static final String LOGIN_URL = "/login";
    private static final String OAUTH2 = "/oauth2";
    private static final String GUEST_URL = "/v1/api/guest";
    private static final String AUTH_URL = "/v1/api/auth";


    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String accessToken = jwtTokenProvider.resolveAccessToken(authorizationHeader);
        jwtTokenProvider.validateAccessToken(accessToken);

        setAuthentication(accessToken);
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String accessToken) {
        Long memberId = jwtTokenProvider.getMemberId(accessToken);
        String role = jwtTokenProvider.getRole(accessToken);
        jwtTokenProvider.checkMemberExist(memberId);

        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(memberId, "",
                List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith(SWAGGER_PATH)
                || path.startsWith(SWAGGER_FAVICON)
                || path.startsWith(SWAGGER_PATH_3)
                || path.startsWith(HEALTH_CHECK_URL)
                || path.startsWith(LOGIN_URL)
                || path.startsWith(OAUTH2)
                || path.startsWith(GUEST_URL)
                || path.startsWith(AUTH_URL)
                ;
    }
}
