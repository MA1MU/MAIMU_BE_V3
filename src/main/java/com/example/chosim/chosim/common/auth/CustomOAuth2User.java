package com.example.chosim.chosim.common.auth;

import com.example.chosim.chosim.domain.auth.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


public class CustomOAuth2User implements OAuth2User {

    private Member member;
    private Map<String, Object> attributes;
    private String attributeKey;

    public CustomOAuth2User(Member member, Map<String,Object> attributes, String attributeKey){
        this.member = member;
        this.attributes = attributes;
        this.attributeKey = attributeKey;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();

        collection.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return member.getRole().getKey();
            }
        });

        return collection;
    }

    @Override
    public String getName() {
        return member.getName();
    }

    public String getProviderId() {
        return member.getProviderId();
    }

    public String getEmail(){
        return member.getEmail();
    }
}
