package com.example.chosim.chosim.common.config;


import com.example.chosim.chosim.common.error.handler.CustomAccessDeniedHandler;
import com.example.chosim.chosim.common.filter.JwtAuthorizationFilter;
import com.example.chosim.chosim.common.filter.JwtExceptionFilter;
import com.example.chosim.chosim.common.jwt.JwtTokenProvider;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import com.example.chosim.chosim.common.auth.CustomOauth2SuccessHandler;
import com.example.chosim.chosim.common.auth.CustomOAuth2UserService;
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
            "/login/**"      // OAuth2 로그인 엔드포인트 추가
    };

    private static final String[] USER_URL = {
            "/v1/api/member/edit",
            "/v1/api/group/**",
            "/v1/api/maimu/**"
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOauth2SuccessHandler customOauth2SuccessHandler;
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
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers(HttpMethod.GET, USER_URL).hasRole("MEMBER")
                        .requestMatchers(HttpMethod.POST,USER_URL).hasRole("MEMBER")
                        .requestMatchers(HttpMethod.PATCH, USER_URL).hasRole("MEMBER")
                        .requestMatchers(HttpMethod.DELETE, USER_URL).hasRole("MEMBER")
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
                        .successHandler(customOauth2SuccessHandler)
//                        .failureHandler(customFailureHandler)
                );

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
