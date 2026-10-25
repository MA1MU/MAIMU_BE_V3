package com.example.chosim.chosim.domain.guest.service;

import com.example.chosim.chosim.api.guest.dto.GuestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 게스트가 초대 링크를 열 때 보는 사물함 요약(이름·색·마이무 개수)의 Cache-aside.
 * 링크가 단톡방에 뿌려지면 같은 사물함을 수백 명이 동시에 연다 → 읽기는 많고 값은 거의 안 바뀐다.
 *
 * 무효화: 새 마이무 DB 반영, 마이무 삭제, 사물함 수정/삭제 시 evict. 그 외 경합으로 남는 오차는 TTL 이 상한이다.
 */
@Component
@RequiredArgsConstructor
public class GuestGroupCache {

    private static final Duration TTL = Duration.ofSeconds(60);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public GuestResponse get(Long groupId, Supplier<GuestResponse> loader) {
        String cached = redisTemplate.opsForValue().get(key(groupId));
        if (cached != null) {
            return read(cached);
        }
        GuestResponse fresh = loader.get();
        redisTemplate.opsForValue().set(key(groupId), write(fresh), TTL);
        return fresh;
    }

    public void evict(Long groupId) {
        redisTemplate.delete(key(groupId));
    }

    private String key(Long groupId) {
        return "guest:group:" + groupId;
    }

    private GuestResponse read(String json) {
        try {
            return objectMapper.readValue(json, GuestResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("사물함 캐시 역직렬화 실패", e);
        }
    }

    private String write(GuestResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("사물함 캐시 직렬화 실패", e);
        }
    }
}
