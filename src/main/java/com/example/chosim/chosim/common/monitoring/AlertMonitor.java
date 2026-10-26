package com.example.chosim.chosim.common.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * 10초마다 지표를 검사해 기준을 넘으면 알린다.
 *
 * 알림 기준 설계:
 *  - 임계값: 지표마다 "이 값을 넘으면 사용자가 곧 느낀다" 는 선.
 *  - 연속 횟수: 순간 튐(GC 직후 CPU 스파이크 등)으로 울리지 않도록 N번 연속 넘어야 울린다.
 *  - 쿨다운: 같은 알림은 10분에 한 번만. 장애 중 메일이 수백 통 쌓이면 정작 중요한 알림을 놓친다.
 *  - 해소 알림: 값이 기준 아래로 돌아오면 "해소" 를 남겨 장애 지속 시간을 알 수 있게 한다.
 *
 * 주의: 여기서는 ERROR 레벨로 로그를 찍지 않는다. ERROR 로그 수 자체가 알림 기준이라 스스로를 부풀리게 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertMonitor {

    static final Duration COOLDOWN = Duration.ofMinutes(10);
    private static final int MAX_HISTORY = 50;

    private final MonitoringService monitoringService;
    private final JavaMailSender mailSender;
    // 기존 메일 발송 풀(ThreadPoolConfig). 빈 이름과 파라미터 이름이 같아서 이 풀이 주입된다
    private final ThreadPoolTaskExecutor emailTaskExecutor;

    @Value("${monitoring.alert-email:}")
    private String alertEmail;

    private long previousErrorCount = -1;

    // 판정 기준 목록. 숫자를 바꾸려면 여기만 고친다
    private final List<AlertRule> rules = List.of(
            new AlertRule("CPU", "프로세스 CPU 사용률", "%", 80, 3, MonitoringService.Metrics::processCpuPercent),
            new AlertRule("HEAP", "힙 메모리 사용률", "%", 85, 3, MonitoringService.Metrics::heapPercent),
            new AlertRule("DB_POOL", "DB 커넥션을 기다리는 스레드", "개", 1, 3, MonitoringService.Metrics::dbWaiting),
            new AlertRule("ERROR_LOG", "10초간 ERROR 로그", "건", 10, 1, this::newErrorLogs),
            new AlertRule("LOG_QUEUE", "비동기 로그 큐 사용률", "%", 80, 1, MonitoringService.Metrics::logQueuePercent),
            new AlertRule("WRITE_BUFFER", "DB 반영 대기 마이무", "건", 1000, 3, MonitoringService.Metrics::writeBufferSize),
            new AlertRule("DEAD_LETTER", "반영 실패로 격리된 마이무", "건", 1, 1, MonitoringService.Metrics::deadLetterSize)
    );

    private final Deque<Alert> history = new ArrayDeque<>();

    // 기동 직후엔 클래스 로딩·JIT 로 CPU 가 튀므로 30초 뒤부터 검사한다
    @Scheduled(initialDelay = 30_000, fixedDelay = 10_000)
    public synchronized void check() {
        MonitoringService.Metrics metrics;
        try {
            metrics = monitoringService.readMetrics();
        } catch (Exception e) {
            log.warn("[ALERT] 지표 수집 실패: {}", e.getMessage());
            return;
        }
        Instant now = Instant.now();
        for (AlertRule rule : rules) {
            double value = rule.metric.applyAsDouble(metrics);
            switch (rule.evaluate(value, now)) {
                case FIRE -> fire(rule, value, now);
                case RECOVER -> record(new Alert(now, rule.name, rule.description + " 정상 복귀 (" + format(value, rule) + ")", true));
                case NONE -> { }
            }
        }
    }

    // ERROR 로그는 누적값이라 직전 검사 이후 늘어난 만큼을 본다
    private double newErrorLogs(MonitoringService.Metrics metrics) {
        long current = metrics.errorLogCount();
        long delta = previousErrorCount < 0 ? 0 : current - previousErrorCount;
        previousErrorCount = current;
        return delta;
    }

    private void fire(AlertRule rule, double value, Instant now) {
        String message = rule.description + " " + format(value, rule) + " (기준 " + format(rule.threshold, rule)
                + ", " + rule.consecutive + "회 연속)";
        record(new Alert(now, rule.name, message, false));
        sendMail(rule.name, message);
    }

    private void record(Alert alert) {
        log.warn("[ALERT{}] {} - {}", alert.recovered() ? " 해소" : "", alert.rule(), alert.message());
        if (history.size() == MAX_HISTORY) {
            history.pollFirst();
        }
        history.addLast(alert);
    }

    /**
     * 이 메서드는 스케줄러 스레드에서 불린다. Spring 기본 스케줄러는 스레드가 1개뿐이고
     * Write-back 플러시(200ms 주기)도 같은 스레드를 쓴다. SMTP 는 수 초 걸릴 수 있으므로
     * 여기서 직접 보내면 그동안 마이무 DB 반영이 멈춘다 → 메일 풀 스레드에 넘기고 바로 돌아온다.
     */
    private void sendMail(String ruleName, String message) {
        if (alertEmail == null || alertEmail.isBlank()) {
            return;
        }
        emailTaskExecutor.execute(() -> {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(alertEmail);
                mail.setSubject("[MAIMU 장애 알림] " + ruleName);
                mail.setText(message + "\n\n관리자 페이지: https://apimaimu.co.kr/admin.html");
                mailSender.send(mail);
            } catch (Exception e) {
                log.warn("[ALERT] 알림 메일 발송 실패: {}", e.getMessage());
            }
        });
    }

    private static String format(double value, AlertRule rule) {
        return (rule.unit.equals("%") ? String.format("%.1f", value) : String.valueOf((long) value)) + rule.unit;
    }

    /** 관리자 페이지용 스냅샷. check() 와 같은 락을 잡아 판정 중간 상태를 읽지 않는다 */
    public synchronized Status status() {
        List<RuleStatus> ruleStatuses = rules.stream()
                .map(r -> new RuleStatus(r.name, r.description, r.threshold, r.unit, r.consecutive,
                        r.lastValue, r.breachCount, r.firing))
                .toList();
        List<Alert> recent = new ArrayList<>(history);
        Collections.reverse(recent);
        return new Status(ruleStatuses, recent);
    }

    public record Alert(Instant at, String rule, String message, boolean recovered) {
    }

    public record RuleStatus(String name, String description, double threshold, String unit, int consecutive,
                             double lastValue, int breachCount, boolean firing) {
    }

    public record Status(List<RuleStatus> rules, List<Alert> history) {
    }

    enum Result { NONE, FIRE, RECOVER }

    /** 규칙 하나의 판정 상태. 시간(now)을 인자로 받아서 테스트에서 시계를 마음대로 돌릴 수 있다 */
    static final class AlertRule {
        final String name;
        final String description;
        final String unit;
        final double threshold;
        final int consecutive;
        final ToDoubleFunction<MonitoringService.Metrics> metric;

        double lastValue;
        int breachCount;
        boolean firing;
        Instant lastFiredAt;

        AlertRule(String name, String description, String unit, double threshold, int consecutive,
                  ToDoubleFunction<MonitoringService.Metrics> metric) {
            this.name = name;
            this.description = description;
            this.unit = unit;
            this.threshold = threshold;
            this.consecutive = consecutive;
            this.metric = metric;
        }

        Result evaluate(double value, Instant now) {
            lastValue = value;
            if (value < threshold) {
                breachCount = 0;
                if (firing) {
                    firing = false;
                    return Result.RECOVER;
                }
                return Result.NONE;
            }
            breachCount++;
            if (breachCount < consecutive) {
                return Result.NONE;
            }
            if (lastFiredAt != null && now.isBefore(lastFiredAt.plus(COOLDOWN))) {
                return Result.NONE;   // 쿨다운 중: 상태는 유지하되 다시 울리지 않는다
            }
            firing = true;
            lastFiredAt = now;
            return Result.FIRE;
        }
    }
}
