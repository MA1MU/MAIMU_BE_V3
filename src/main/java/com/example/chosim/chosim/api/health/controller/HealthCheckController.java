package com.example.chosim.chosim.api.health.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "ELB 헬스 체크 API", description = "ELB의 health 체크 기능으로 연결 유지")
public class HealthCheckController {

    @GetMapping("/health")
    @Operation(summary = "ELB 헬스 체크",description = "ELB의 health 체크 기능으로 연결 유지")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
