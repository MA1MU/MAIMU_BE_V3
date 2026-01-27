package com.example.chosim.chosim.common.invitation;

import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "invitation", timeToLive = 86400) //1일 60*60*24
public class Invitation {

    @Id
    private String token; // UUID가 저장될 필드 (Redis Key가 됨)

    @Indexed
    private Long groupId; // 특정 그룹 ID로 검색이 필요할 경우 @Indexed 추가

    private Long senderId;
}
