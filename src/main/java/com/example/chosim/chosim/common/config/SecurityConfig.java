package com.example.chosim.chosim.common.config;


import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
//import com.example.chosim.chosim.jwt.JWTFilter;
import com.example.chosim.chosim.common.jwt.JWTUtil;
import com.example.chosim.chosim.common.auth.CustomOauth2SuccessHandler;
import com.example.chosim.chosim.common.auth.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOauth2SuccessHandler customOauth2SuccessHandler;
    private final JWTUtil jwtUtil;

    private final MemberRepository memberRepository;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, CustomOauth2SuccessHandler customOauth2SuccessHandler, JWTUtil jwtUtil, MemberRepository memberRepository){
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOauth2SuccessHandler = customOauth2SuccessHandler;
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    private static final String[] WHITE_LIST = {
            "/**",
            "/user/join",
            "/v1/api/guest/**",
            "/login/**",
            "/user/validate"
    };

    private static final String[] USER_URL = {
            "/v1/api/group",
            "/v1/api/group/**",
            "user/**",
            "/v1/api/maimu/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {


        //경로별 인가 작업
        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers(HttpMethod.GET).permitAll()
                        .requestMatchers(HttpMethod.GET,USER_URL).authenticated()
                        .requestMatchers(HttpMethod.POST,USER_URL).authenticated()
                        .requestMatchers(HttpMethod.PATCH, USER_URL).authenticated()
                        .requestMatchers(HttpMethod.DELETE, USER_URL).authenticated()
                        .anyRequest().authenticated());
        //csrf disable
        http
                .csrf(AbstractHttpConfigurer::disable);
        //From 로그인 방식 disable
        http
                .formLogin(AbstractHttpConfigurer::disable);

        //HTTP Basic 인증 방식 disable
        http
                .httpBasic(AbstractHttpConfigurer::disable);
        http
                .logout(AbstractHttpConfigurer::disable);

        http
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, memberRepository), UsernamePasswordAuthenticationFilter.class);
//        http
//                .addFilterAfter(new JWTFilter(jwtUtil), OAuth2LoginAuthenticationFilter.class);

        //oauth2
        //failure handler 추가 필요
        http
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig
                                .userService(customOAuth2UserService))
                        .successHandler(customOauth2SuccessHandler)
                        .failureHandler(customFailureHandler)
                );



//        세션 설정 : STATELESS
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
