package com.example.chosim.chosim.oauth2.handler;

import com.example.chosim.chosim.jwt.JWTUtil;
import com.example.chosim.chosim.oauth2.dto.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    public CustomSuccessHandler(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        //OAuth2User
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        System.out.println(customUserDetails);

        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        System.out.println(authorities);
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();
        System.out.println("Role is" + role);
        String token = jwtUtil.createJwt(username, role);

        response.addCookie(createCookie("Authorization", token));
        response.sendRedirect("http://localhost:8080/v1/api/join/test");

        /**
         * ROLE_GUEST(초기 로그인 이면 프로필 에딧으로
         * ROLE_USER(한번 로그인 했으면 main page로
         */
//        if(role == "ROLE_GUEST") {
//            //여기가 원래는 프론트엔드 주소
//            response.sendRedirect("http://localhost:3000/ProfileEdit");
////              response.sendRedirect("http://localhost:8080/v1/api/join/test");
//        }
//
//        else{
//            response.sendRedirect("http://localhost:3000/MainPage");
////            response.sendRedirect("http://localhost:8080/my");
//        }
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60*60*60);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }

}
