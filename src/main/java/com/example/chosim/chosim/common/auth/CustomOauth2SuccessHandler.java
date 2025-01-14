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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Iterator;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOauth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final int REFRESH_TOKEN_AGE = 259200;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    private final String REGISTER_URL= "http://localhost:3000/ProfileEdit";
    private final String MAINPAGE_URL = "http://localhost:3000/MainPage";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //OAuth2User
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        String grantedAuthority = authentication.getAuthorities().stream()
                .findAny()
                .orElseThrow()
                .toString();
        MemberRole checkRole = MemberRole.fromKey(grantedAuthority);
        Token token = jwtTokenProvider.createToken(principal.getMember().getId(), grantedAuthority);

        String redirectUrl = getRedirectUrlByRole(checkRole);

        //MemberRole에 따른 분기 처리
        if (checkRole == MemberRole.PREMEMBER) {
            String tempToken = token.getAccessToken();
            response.addHeader("tempToken", tempToken);
            log.info("회원가입 페이지로 redirect, JWT 임시 토큰 생성");
        }
        else {
            String accessToken = token.getAccessToken();
            response.addHeader("accessToken", accessToken);

            RefreshToken refreshToken = new RefreshToken(principal.getMember().getId(), token.getRefreshToken());
            refreshToken.updateRefreshToken(token.getRefreshToken());
            refreshTokenRepository.save(refreshToken);

            Cookie cookie = new Cookie("refreshToken", token.getRefreshToken());
            cookie.setPath("/");
            ZonedDateTime seoulTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
            ZonedDateTime expirationTime = seoulTime.plusSeconds(REFRESH_TOKEN_AGE);
            cookie.setMaxAge((int) (expirationTime.toEpochSecond() - seoulTime.toEpochSecond()));
            cookie.setSecure(true);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);

            log.info("기존 회원 로그인 성공, accessToken 및 refreshToken 생성");
        }
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String getRedirectUrlByRole(MemberRole role){

        if (role == MemberRole.PREMEMBER) {
            return UriComponentsBuilder.fromUriString(REGISTER_URL)
                    .build()
                    .toUriString();
        }
        return UriComponentsBuilder.fromUriString(MAINPAGE_URL)
                .build()
                .toUriString();
    }
}
