package com.example.chosim.chosim.domain.auth.service;

import com.example.chosim.chosim.api.auth.dto.LogoutRequest;
import com.example.chosim.chosim.api.auth.dto.ReissueResponse;
import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.common.error.exception.AppException;
import com.example.chosim.chosim.common.jwt.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN = "refreshToken";

    private final JwtTokenProvider jwtTokenProvider;
    private final BlackListRepository blackListRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration}")
    private int refreshTokenAge;

    @Transactional
    public ReissueResponse reissue(String refreshToken, HttpServletResponse response) {
        if (jwtTokenProvider.isExpired(refreshToken) || blackListRepository.existsById(refreshToken)) {
            log.warn("블랙리스트에 존재하는 토큰: {}", blackListRepository.existsById(refreshToken));
            throw new AppException(ErrorCode.REISSUE_FAIL);
        }
        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        String role = jwtTokenProvider.getRole(refreshToken);

        RefreshToken findToken = refreshTokenRepository.findById(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.LOGIN_ID_NOT_FOUND));

        log.info("[브라우저에서 들어온 쿠키] == [DB에 저장된 토큰], {}", refreshToken.equals(findToken.getRefreshToken()));
        if (!refreshToken.equals(findToken.getRefreshToken())) {
            log.warn("[쿠키로 들어온 토큰과 DB의 토큰이 일치하지 않음.]");
            throw new AppException(ErrorCode.REFRESH_TOKEN_NOT_EXIST);
        }
        jwtTokenProvider.setBlackList(refreshToken);
        Token token = jwtTokenProvider.createToken(memberId, role);
        findToken.updateRefreshToken(token.getRefreshToken());
        refreshTokenRepository.save(findToken);

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN, token.getRefreshToken());
        refreshCookie.setMaxAge(refreshTokenAge / 1000);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        response.addCookie(refreshCookie);
        //TODO: accessToken을 header response로 바꿔야 할수도 있음, 프론트 사정에 따라서
        return new ReissueResponse(token.getAccessToken());
    }

    @Transactional
    public void logout(LogoutRequest request, String refreshToken, HttpServletResponse response) {
        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        RefreshToken existRefreshToken = refreshTokenRepository.findById(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_EXIST));

        jwtTokenProvider.setBlackList(refreshToken);
        log.info("[로그아웃 된 리프레시 토큰 블랙리스트 처리]");
        refreshTokenRepository.delete(existRefreshToken);
        Cookie deleteCookie = new Cookie(REFRESH_TOKEN, null);
        deleteCookie.setMaxAge(0);
        deleteCookie.setPath("/");
        deleteCookie.setSecure(true);
        deleteCookie.setHttpOnly(true);
        response.addCookie(deleteCookie);
        jwtTokenProvider.setBlackList(request.getAccessToken());
        log.info("[로그아웃 된 액세스 토큰 블랙리스트 처리]");
    }
}
