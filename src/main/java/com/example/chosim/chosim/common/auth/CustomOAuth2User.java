package com.example.chosim.chosim.common.auth;

import com.example.chosim.chosim.domain.auth.entity.Member;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
public class CustomOAuth2User implements OAuth2User {

    private final Member member;
    private final Map<String, Object> attributes;
    private final String attributeKey;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(member.getRole().getKey()));
    }

    @Override
    public String getName() {
        return member.getName();
    }

    public String getProvider() {
        return member.getProvider();
    }

    public String getProviderId() {
        return member.getProviderId();
    }

    public String getEmail(){
        return member.getEmail();
    }
}
