package com.example.chosim.chosim.common.auth;

import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.common.error.exception.AppException;
import com.example.chosim.chosim.common.jwt.JwtTokenProvider;
import com.example.chosim.chosim.common.jwt.RefreshToken;
import com.example.chosim.chosim.common.jwt.RefreshTokenRepository;
import com.example.chosim.chosim.common.jwt.Token;
import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.enums.MemberRole;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final String REGISTER_URL= "http://localhost:3000/ProfileEdit";
    private final String MAINPAGE_URL = "http://localhost:3000/MainPage";

    public CustomOauth2SuccessHandler(JwtTokenProvider jwtTokenProvider, MemberRepository memberRepository, RefreshTokenRepository refreshTokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        //OAuth2User
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        System.out.println(oAuth2User);

        String providerId = oAuth2User.getProviderId();

        Member member = memberRepository.findByProviderId(providerId).orElseThrow(()-> new AppException(ErrorCode.MEMBER_NOT_FOUND));

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();
        System.out.println("Role is" + role);

        String redirectUrl = getRedirectUrlByRole(MemberRole.fromKey(role));
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        Token token = jwtTokenProvider.createToken(member.getId(), role);

        //Cookie 생성
        Cookie cookie = new Cookie("refreshtoken", token.getRefreshToken());
        // 쿠키 속성 설정
        cookie.setHttpOnly(true);  //httponly 옵션 설정
        cookie.setSecure(true); //https 옵션 설정
        cookie.setPath("/"); // 모든 곳에서 쿠키열람이 가능하도록 설정
        cookie.setMaxAge(60 * 60 * 24); //쿠키 만료시간 설정

        response.setHeader("accesstoken", token.getAccessToken());
        response.addCookie(cookie);

        //RefreshToken Reddis 저장
        RefreshToken refreshToken = new RefreshToken(member.getId(),token.getRefreshToken());
        refreshTokenRepository.save(refreshToken);

        getRedirectStrategy().sendRedirect(request,response,redirectUrl);
    }

    private String getRedirectUrlByRole(MemberRole role){

        if (role == MemberRole.PREMEMBER) {
            return UriComponentsBuilder.fromUriString(REGISTER_URL)
                    .build()
                    .toUriString();
        }
        return UriComponentsBuilder.fromHttpUrl(MAINPAGE_URL)
                .build()
                .toUriString();
    }
}
