package com.example.chosim.chosim.common.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class EmailSchedule {

    private final EmailService emailService;

    @Scheduled(cron = "0 * * * * *")
    public void scheduleMaimuNotification() {
        log.info("MAIMU 요약 알림 스케줄러 실행 (오전 10시 ~ 오후 10시 정각)");
        try {
            emailService.sendSummaryEmails();
            log.info("MAIMU 요약 알림 발송 프로세스 완료!");
        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생: {}", e.getMessage());
        }
    }
}
