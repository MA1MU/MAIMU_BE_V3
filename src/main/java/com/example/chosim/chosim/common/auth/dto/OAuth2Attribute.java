package com.example.chosim.chosim.common.auth.dto;

import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.enums.MemberRole;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuth2Attribute {

    private Map<String, Object> attributes;
    private String username;
    private String email;
    private String providerId;

    public static OAuth2Attribute of(String provider, Map<String, Object> attributes) {
        if (provider.equals("google")) {
            return ofGoogle(attributes);
        } else if (provider.equals("kakao")) {
            return ofKakao(attributes);
        } else if (provider.equals("naver")) {
            return ofNaver(attributes);
        }
        return null;
    }

    private static OAuth2Attribute ofGoogle(Map<String, Object> attributes) {
        return OAuth2Attribute.builder()
                .username(attributes.get("name").toString())
                .email(attributes.get("email").toString())
                .providerId(attributes.get("sub").toString())
                .attributes(attributes)
                .build();
    }

    private static OAuth2Attribute ofKakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>)attributes.get("kakao_account");  // 카카오로 받은 데이터에서 계정 정보가 담긴 kakao_account 값을 꺼낸다.
        Map<String, Object> profile = (Map<String, Object>)kakaoAccount.get("profile");   // 마찬가지로 profile(nickname, image_url.. 등) 정보가 담긴 값을 꺼낸다.
        return OAuth2Attribute.builder()
                .username(profile.get("nickname").toString())
                .email(kakaoAccount.get("email").toString())
                .providerId(attributes.get("id").toString())
                .attributes(attributes)
                .build();
    }

    private static OAuth2Attribute ofNaver(Map<String, Object> attributes) {
        Map<String, Object> attributesMap = (Map<String, Object>)attributes.get("response");
        return OAuth2Attribute.builder()
                .username(attributesMap.get("name").toString())
                .email(attributesMap.get("email").toString())
                .providerId(attributesMap.get("id").toString())
                .attributes(attributesMap)
                .build();
    }

}
