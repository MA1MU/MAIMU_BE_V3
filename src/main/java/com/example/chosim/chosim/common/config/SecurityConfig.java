package com.example.chosim.chosim.common.config;


import com.example.chosim.chosim.common.auth.CustomOauth2FailureHandler;
import com.example.chosim.chosim.common.error.handler.CustomAccessDeniedHandler;
import com.example.chosim.chosim.common.filter.JwtAuthorizationFilter;
import com.example.chosim.chosim.common.filter.JwtExceptionFilter;
import com.example.chosim.chosim.common.jwt.JwtTokenProvider;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import com.example.chosim.chosim.common.auth.CustomOauth2SuccessHandler;
import com.example.chosim.chosim.common.auth.CustomOAuth2UserService;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.CorsFilter;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] WHITE_LIST = {
//            "/**",
            "/v1/api/guest/**",
            "/v1/api/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/health",
            "/oauth2/**",      // OAuth2 관련 엔드포인트 추가
            "/login/**",      // OAuth2 로그인 엔드포인트 추가
            "/v1/api/guest",
            "/admin.html"     // 관리자 페이지 껍데기(정적 HTML). 데이터 API 는 아래에서 ADMIN 으로 막는다
    };

    private static final String[] USER_URL = {
            "/v1/api/member/edit",
            "/v1/api/group/**",
            "/v1/api/maimu/**",
            "/v1/api/notification/**"
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOauth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomOauth2FailureHandler customOAuth2FailureHandler;
    private final JwtAuthorizationFilter jwtAuthorizationFilter;
    private final MemberRepository memberRepository;
    private final CorsFilter corsFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception ->
                        exception.accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        // ERROR: 403/500 을 sendError 로 보내면 /error 로 다시 디스패치되는데, 이 내부 요청엔 인증 정보가 없다.
                        // 막혀 있으면 원래 403 대신 로그인 페이지(302)로 튕긴다. 원 요청은 이미 인가 검사를 거쳤으므로 열어 둔다.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers("/v1/api/member/join").hasRole("PREMEMBER")
                        .requestMatchers("/v1/api/admin/**").hasRole("ADMIN")
                        // 관리자도 일반 회원 기능은 그대로 쓸 수 있어야 한다
                        .requestMatchers(HttpMethod.GET, USER_URL).hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.POST,USER_URL).hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, USER_URL).hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, USER_URL).hasAnyRole("MEMBER", "ADMIN")
                        .anyRequest().authenticated());

        http
                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtExceptionFilter(), JwtAuthorizationFilter.class)
                .addFilterBefore(corsFilter, JwtExceptionFilter.class);


        //TODO: failure handler 추가 필요
        http
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig
                                .userService(customOAuth2UserService))
                        .successHandler(customOAuth2SuccessHandler)
                        .failureHandler(customOAuth2FailureHandler)
                );

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
