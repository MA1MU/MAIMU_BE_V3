package com.example.chosim.chosim.common.jwt;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@Getter
@Builder
@RedisHash(value = "blackList")
public class BlackList {

    @Id
    private String id;

    @TimeToLive(unit = TimeUnit.MICROSECONDS)
    private Long ttl;
}
