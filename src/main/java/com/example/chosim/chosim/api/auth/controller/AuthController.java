package com.example.chosim.chosim.api.auth.controller;

import com.example.chosim.chosim.api.auth.dto.LogoutRequest;
import com.example.chosim.chosim.api.auth.dto.ReissueResponse;
import com.example.chosim.chosim.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "인증 관련 API", description = "회원 인증 관련 API 모음")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "액세스 토큰 만료시 리프레시 토큰을 사용해 재발급합니다.")
    public ResponseEntity<ReissueResponse> tokenReissue(@CookieValue(name = "refreshToken") String refreshToken,
                                                        HttpServletResponse response) {
        log.info("[액세스 토큰 재발급 컨트롤러]: 쿠키 존재 여부, {}", !refreshToken.isEmpty());
        return ResponseEntity.ok(authService.reissue(refreshToken, response));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃을 진행합니다.")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken") String refreshToken,
                                       @RequestBody @Valid LogoutRequest request, HttpServletResponse response) {
        authService.logout(request, refreshToken, response);
        return ResponseEntity.noContent().build();
    }
    
    //TODO: 이메일 찾기

}
