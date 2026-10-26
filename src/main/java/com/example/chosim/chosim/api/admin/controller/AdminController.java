package com.example.chosim.chosim.api.admin.controller;

import com.example.chosim.chosim.common.monitoring.AlertMonitor;
import com.example.chosim.chosim.common.monitoring.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/** 관리자 페이지(/admin.html)가 호출하는 API. SecurityConfig 에서 ROLE_ADMIN 만 허용한다. */
@Tag(name = "관리자 API", description = "로그 조회, 서버 모니터링, 장애 알림")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/admin")
public class AdminController {

    private final MonitoringService monitoringService;
    private final AlertMonitor alertMonitor;

    @GetMapping("/logs")
    @Operation(summary = "로그 조회", description = "로그 파일의 마지막 N줄(최대 1000)을 반환합니다. keyword 로 traceId·ERROR 등을 거를 수 있습니다.")
    public ResponseEntity<List<String>> logs(@RequestParam(defaultValue = "200") int lines,
                                             @RequestParam(required = false) String keyword) throws IOException {
        return ResponseEntity.ok(monitoringService.readLogs(lines, keyword));
    }

    @GetMapping("/metrics")
    @Operation(summary = "서버 지표", description = "CPU, 힙·GC, DB 커넥션 풀, 스레드 상태, ERROR 로그 수, 로그 큐, Write-back 대기열을 반환합니다.")
    public ResponseEntity<MonitoringService.Metrics> metrics() {
        return ResponseEntity.ok(monitoringService.readMetrics());
    }

    @GetMapping("/alerts")
    @Operation(summary = "알림 현황", description = "알림 기준별 현재 상태와 최근 알림 50건을 반환합니다.")
    public ResponseEntity<AlertMonitor.Status> alerts() {
        return ResponseEntity.ok(alertMonitor.status());
    }
}
