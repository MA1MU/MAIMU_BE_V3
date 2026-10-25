package com.example.chosim.chosim.api.notification.controller;

import com.example.chosim.chosim.common.sse.SseEmitters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/api/notification")
@RequiredArgsConstructor
@Tag(name = "알림 관련 API", description = "접속 중인 사용자에게 새 마이무 도착을 실시간(SSE)으로 알린다.")
public class NotificationController {

    private final SseEmitters sseEmitters;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "실시간 알림 구독", description = "SSE 연결을 열어두면 내 사물함에 새 마이무가 저장될 때마다 maimu 이벤트를 받습니다.")
    public SseEmitter subscribe(@AuthenticationPrincipal Long memberId, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no"); // nginx 등 프록시의 응답 버퍼링을 끈다
        return sseEmitters.subscribe(memberId);
    }
}
