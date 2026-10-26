package com.example.chosim.chosim.common.monitoring;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.example.chosim.chosim.domain.maimu.service.MaimuWriteBackBuffer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * 관리자 페이지와 알림(AlertMonitor)이 쓰는 두 가지를 만든다.
 * 1) 로그: logback 이 쓰는 파일의 마지막 N 줄 (키워드/traceId 필터)
 * 2) 지표: Actuator 가 Micrometer MeterRegistry 에 자동 등록한 값(CPU·힙·GC·스레드·커넥션 풀·ERROR 로그 수)을 한 번에 읽는다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private static final int MAX_LINES = 1000;

    private final MeterRegistry registry;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${LOG_PATH:logs}")
    private String logPath;

    public List<String> readLogs(int lines, String keyword) throws IOException {
        int limit = Math.min(Math.max(lines, 1), MAX_LINES);
        return tail(Path.of(logPath, "maimu.log"), limit, keyword);
    }

    public Metrics readMetrics() {
        long gcCount = 0;
        long gcTimeMs = 0;
        for (Timer gc : registry.find("jvm.gc.pause").timers()) {
            gcCount += gc.count();
            gcTimeMs += (long) gc.totalTime(TimeUnit.MILLISECONDS);
        }
        Counter errors = registry.find("logback.events").tag("level", "error").counter();
        AsyncAppender logQueue = asyncFileAppender();

        return new Metrics(
                gauge("process.cpu.usage") * 100,
                gauge("system.cpu.usage") * 100,
                toMb(gauge("jvm.memory.used", "area", "heap")),
                toMb(gauge("jvm.memory.max", "area", "heap")),
                gcCount,
                gcTimeMs,
                (int) gauge("hikaricp.connections.active"),
                (int) gauge("hikaricp.connections.idle"),
                (int) gauge("hikaricp.connections.pending"),
                (int) gauge("hikaricp.connections.max"),
                threadStates(),
                errors == null ? 0 : (long) errors.count(),
                logQueue == null ? 0 : logQueue.getNumberOfElementsInQueue(),
                logQueue == null ? 0 : logQueue.getQueueSize(),
                (long) gauge("process.uptime"),
                size(MaimuWriteBackBuffer.BUFFER_KEY),
                size(MaimuWriteBackBuffer.DEAD_LETTER_KEY)
        );
    }

    /**
     * 파일을 끝까지 읽으며 마지막 limit 줄만 Deque 에 남긴다(메모리는 limit 줄만 사용).
     * 스택트레이스처럼 날짜로 시작하지 않는 줄은 바로 앞 로그의 일부로 보고, 앞 줄이 필터에 걸렸으면 같이 보여준다.
     * ponytail: 매 요청마다 현재 파일(최대 10MB)을 처음부터 읽는다. 더 커지면 파일 끝에서 거꾸로 읽거나 Loki/ELK 로 옮긴다.
     */
    static List<String> tail(Path file, int limit, String keyword) throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        boolean filter = keyword != null && !keyword.isBlank();
        Deque<String> last = new ArrayDeque<>(limit);
        boolean matching = !filter;
        // InputStreamReader 는 깨진 바이트(쓰는 중인 마지막 줄)를 예외 대신 대체 문자로 바꿔준다
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                boolean newEvent = !line.isEmpty() && Character.isDigit(line.charAt(0));
                if (newEvent && filter) {
                    matching = line.contains(keyword);
                }
                if (!matching) {
                    continue;
                }
                if (last.size() == limit) {
                    last.pollFirst();
                }
                last.addLast(line);
            }
        }
        return new ArrayList<>(last);
    }

    // 같은 이름의 지표가 여러 개(예: 힙 영역별 Eden/Old/Survivor)면 합친다. 값이 없거나 -1(미정)인 항목은 뺀다
    private double gauge(String name, String... tags) {
        return registry.find(name).tags(tags).gauges().stream()
                .mapToDouble(Gauge::value)
                .filter(v -> !Double.isNaN(v) && v >= 0)
                .sum();
    }

    // 스레드 상태별 개수. blocked 가 늘면 synchronized 락 경합, waiting 이 늘면 풀/큐에서 무언가를 기다리는 중
    private Map<String, Integer> threadStates() {
        Map<String, Integer> states = new TreeMap<>();
        for (Gauge g : registry.find("jvm.threads.states").gauges()) {
            int count = (int) g.value();
            if (count > 0) {
                states.put(g.getId().getTag("state"), count);
            }
        }
        return states;
    }

    // logback-spring.xml 의 ASYNC_FILE. 큐가 꽉 차가면 로그 스레드가 생산 속도를 못 따라간다는 뜻이다
    private static AsyncAppender asyncFileAppender() {
        Logger root = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(Logger.ROOT_LOGGER_NAME);
        return root.getAppender("ASYNC_FILE") instanceof AsyncAppender async ? async : null;
    }

    // Redis 가 죽어도 나머지 지표는 보여야 하므로 실패는 -1 로 표시한다
    private long size(String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            return size == null ? 0 : size;
        } catch (Exception e) {
            log.warn("[모니터링] Redis 조회 실패: {}", e.getMessage());
            return -1;
        }
    }

    private static long toMb(double bytes) {
        return (long) (bytes / 1024 / 1024);
    }

    public record Metrics(
            double processCpuPercent,   // 이 JVM 프로세스의 CPU 사용률
            double systemCpuPercent,    // 서버(컨테이너) 전체 CPU 사용률
            long heapUsedMb,
            long heapMaxMb,
            long gcCount,               // 기동 후 누적 GC 횟수
            long gcTimeMs,              // 기동 후 누적 GC 시간
            int dbActive,               // 지금 쿼리를 실행 중인 커넥션
            int dbIdle,                 // 놀고 있는 커넥션
            int dbWaiting,              // 커넥션을 못 얻고 기다리는 스레드 — 0보다 크면 풀이 부족하다는 신호
            int dbMax,
            Map<String, Integer> threadStates,
            long errorLogCount,         // 기동 후 누적 ERROR 로그 수
            int logQueueUsed,           // 비동기 로그 큐에 쌓여 아직 파일에 안 쓰인 이벤트 수
            int logQueueCapacity,
            long uptimeSeconds,
            long writeBufferSize,       // DB 반영을 기다리는 마이무 수(Write-back 대기열), -1 이면 Redis 조회 실패
            long deadLetterSize         // 반영에 실패해 격리된 마이무 수
    ) {
        double heapPercent() {
            return heapMaxMb > 0 ? heapUsedMb * 100.0 / heapMaxMb : 0;
        }

        double logQueuePercent() {
            return logQueueCapacity > 0 ? logQueueUsed * 100.0 / logQueueCapacity : 0;
        }
    }
}
