package com.example.chosim.chosim.common.auth;

import com.example.chosim.chosim.common.jwt.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

@Component
public class CustomOauth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    public CustomOauth2SuccessHandler(JWTUtil jwtUtil) {
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
        String accessToken = jwtUtil.createJwt(username, role);
//        response.addCookie(createCookie("Authorization", token));
//        response.sendRedirect("http://localhost:3000");
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/LoginHandler")
                            .queryParam("accessToken", accessToken)
                                    .build()
                                            .encode(StandardCharsets.UTF_8)
                                                    .toUriString();
            getRedirectStrategy().sendRedirect(request,response,targetUrl);

        /**
         * ROLE_GUEST(초기 로그인 이면 프로필 에딧으로
         * ROLE_USER(한번 로그인 했으면 main page로
         */
//        if(role.equals("ROLE_GUEST")) {
//            //여기가 원래는 프론트엔드 주소
//            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/ProfileEdit")
//                            .queryParam("accessToken", accessToken)
//                                    .build()
//                                            .encode(StandardCharsets.UTF_8)
//                                                    .toUriString();
//            getRedirectStrategy().sendRedirect(request,response,targetUrl);
////            response.sendRedirect("http://localhost:3000/ProfileEdit");
////              response.sendRedirect("http://localhost:8080/v1/api/join/test");
//        }
//
//        else{
//            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/MainPage")
//                    .queryParam("accessToken", accessToken)
//                    .build()
//                    .encode(StandardCharsets.UTF_8)
//                    .toUriString();
//            getRedirectStrategy().sendRedirect(request,response,targetUrl);
////            response.sendRedirect("http://localhost:3000/MainPage");
////            response.sendRedirect("http://localhost:8080/my");
//        }
    }


}
