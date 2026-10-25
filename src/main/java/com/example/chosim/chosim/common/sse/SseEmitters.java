package com.example.chosim.chosim.common.sse;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 회원별 SSE 연결 보관소. 한 회원이 탭을 여러 개 열 수 있어 회원 id 하나에 연결 여러 개를 둔다.
 *
 * ponytail: 연결을 이 JVM 메모리에 들고 있으므로 단일 인스턴스 전제다.
 * 서버를 여러 대로 늘리면 알림을 Redis Pub/Sub 으로 브로드캐스트하고 각 서버가 자기 연결에만 보낸다.
 */
@Component
public class SseEmitters {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 끊기면 클라이언트가 다시 연결한다

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(memberId, id -> ConcurrentHashMap.newKeySet()).add(emitter);

        Runnable remove = () -> emitters.computeIfPresent(memberId, (id, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;
        });
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        // 첫 이벤트를 바로 보내야 프록시가 응답 헤더를 흘려보내고 클라이언트도 연결 성공을 안다
        send(emitter, SseEmitter.event().name("connect").data("connected"));
        return emitter;
    }

    public void send(Long memberId, String eventName, Object data) {
        Set<SseEmitter> targets = emitters.get(memberId);
        if (targets == null) {
            return; // 접속 중이 아니면 이메일 다이제스트가 대신 알린다
        }
        // SseEventBuilder 는 build() 때마다 내부 버퍼가 바뀌므로 연결마다 새로 만든다
        targets.forEach(e -> send(e, SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON)));
    }

    // 로드밸런서/프록시의 유휴 연결 끊김(보통 60초)을 막는 주석 이벤트
    @Scheduled(fixedRate = 30_000)
    public void heartbeat() {
        emitters.values().forEach(set -> set.forEach(e -> send(e, SseEmitter.event().comment("ping"))));
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e); // onError → remove 로 정리된다
        }
    }
}
