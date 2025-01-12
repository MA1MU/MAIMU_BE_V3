package com.example.chosim.chosim.common.auth;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo{

    private final Map<String, Object> attribute;


    public KakaoUserInfo(Map<String, Object> attribute) {
        this.attribute = attribute;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return attribute.get("id").toString();
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attribute.get("kakao_account");

        return response.get("email").toString();
    }

    @Override
    public String getName() {
        Map<String, Object> account = (Map<String, Object>) attribute.get("properties");
        return account.get("nickname").toString();
    }
}
