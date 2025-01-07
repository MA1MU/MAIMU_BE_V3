package com.example.chosim.chosim.domain.auth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum MemberRole {

    PREMEMBER("ROLE_PREMEMBER", "프로필 입력 전 회원"),
    MEMBER("ROLE_MEMBER", "일반 회원"),
    ADMIN("ROLE_ADMIN", "관리자"),
    SENDER("ROLE_GUEST", "발신자"),
    OLD_MEMBER("ROLE_OM", "탈퇴한 회원"),
    SUSPENDED_MEMBER("ROLE_SM", "정지한 회원");

    private static final Map<String, MemberRole> ROLE_KEY_MAP = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(MemberRole::getKey, Function.identity()));

    private final String key;
    private final String description;

    public static MemberRole fromKey(final String key) {
        MemberRole result = ROLE_KEY_MAP.get(key);
        if (result == null) {
            throw new IllegalArgumentException(String.format("요청한 key(%s)를 찾을 수 없습니다.", key));
        }
        return result;
    }
}
