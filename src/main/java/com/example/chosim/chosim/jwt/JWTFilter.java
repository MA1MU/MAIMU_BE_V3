package com.example.chosim.chosim.jwt;

import com.example.chosim.chosim.oauth2.dto.CustomOAuth2User;
import com.example.chosim.chosim.oauth2.dto.UserDTO;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;


@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.info(String.valueOf(request));

        try {
            authenticate(getAccessToken(request), request, response);
        } catch (JwtException e) {
            log.warn(e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String accessToken, HttpServletRequest request, HttpServletResponse response) {
        try {
            String username = jwtUtil.getUsername(accessToken);
            String role = jwtUtil.getRole(accessToken);
            //userDTO를 생성하여 값 set
            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(username);
            userDTO.setRole(role);
            //UserDetails에 회원 정보 객체 담기
            CustomOAuth2User customOAuth2User = new CustomOAuth2User(userDTO);

            //스프링 시큐리티 인증 토큰 생성
            Authentication authToken = new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());
            //세션에 사용자 등록
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch(ExpiredJwtException e){
            String username = jwtUtil.getUsername(accessToken);
            String role = jwtUtil.getRole(accessToken);
            Cookie cookie = createCookie("Authorization", jwtUtil.createJwt(username,role));
            response.addCookie(cookie);
    }

}

    private String getAccessToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> cookie.getName().equals("Authentication"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElseThrow(() -> new JwtException("AccessToken not found"));
        } else {
            throw new JwtException("AccessToken is not found.");
        }
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60*60*60);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        String[] excludePath = {"/", "/swagger-ui.html"};
        String path = request.getRequestURI();
        return Arrays.stream(excludePath).anyMatch(path::startsWith);
    }

}
