package com.example.chosim.chosim.api.auth.controller;

import com.example.chosim.chosim.common.jwt.JwtTokenProvider;
import com.example.chosim.chosim.domain.auth.service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "인증 관련 API", description = "회원 인증 관련 API 모음")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/auth")
public class AuthController {




}
