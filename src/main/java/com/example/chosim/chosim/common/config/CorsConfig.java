package com.example.chosim.chosim.common.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.addAllowedOrigin("https://maimu.vercel.app");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setExposedHeaders(List.of("accessToken")); //accessToken이라는 이름의 헤더 프론트엔드가 읽을 수 있음
        config.addExposedHeader("Set-Cookie"); //브라우저가 쿠키 설정 헤더를 볼 수 있음
        config.setMaxAge(3600L); //Cors검사 결과를 3600초동안 캐시에 저장

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
