package com.example.chosim.chosim.common.filter;


import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import com.example.chosim.chosim.common.jwt.JWTUtil;
import com.example.chosim.chosim.common.auth.CustomOAuth2User;
import com.example.chosim.chosim.common.auth.UserDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final JWTUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = request.getHeader("Authorization");

        //토크 검사 생략(비어있으면 모두 허용 URL 통과)
        if(!StringUtils.hasText(accessToken)){
            doFilter(request,response,filterChain);
            return;
        }

        //AccessToken 만료 시 예외 발생
        if(jwtUtil.isExpired(accessToken)){
            throw new JwtException("Access Token 만료!");
        }

        if(!jwtUtil.isExpired(accessToken)){
            Member user = memberRepository.findByUsername(jwtUtil.getUsername(accessToken))
                    .orElseThrow(IllegalStateException::new);

            UserDTO userDTO = UserDTO.builder()
                    .role(user.getRole())
                    .name(user.getName())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();

            CustomOAuth2User customOAuth2User = new CustomOAuth2User(userDTO);
            //스프링 시큐리티 인증 토큰 생성
            Authentication authToken = new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());
            //세션에 사용자 등록
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
