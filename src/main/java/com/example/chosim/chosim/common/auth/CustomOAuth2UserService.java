package com.example.chosim.chosim.common.auth;


import com.example.chosim.chosim.common.auth.dto.OAuth2Attribute;
import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.enums.MemberRole;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{

        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId(); //naver, kakao, google
        OAuth2Attribute attribute = OAuth2Attribute.of(provider, oAuth2User.getAttributes());
        String userNameAttributeKey = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        Member member = saveOrUpdate(provider, attribute);
        return new CustomOAuth2User(member, oAuth2User.getAttributes(), userNameAttributeKey);
    }

    private Member saveOrUpdate(String provider, OAuth2Attribute attribute) {
        return memberRepository.findByProviderAndProviderId(provider, attribute.getProviderId())
                .map(existingMember -> updateIfChanged(existingMember, attribute))
                .orElseGet(() -> createNewMember(provider, attribute));
    }

    private Member updateIfChanged(Member existingMember, OAuth2Attribute attribute) {
        boolean isChanged = false;

        if (!existingMember.getEmail().equals(attribute.getEmail())) {
            existingMember.updateEmail(attribute.getEmail());
            isChanged = true;
        }
        if (!existingMember.getName().equals(attribute.getUsername())) {
            existingMember.updateName(attribute.getUsername());
            isChanged = true;
        }

        return isChanged ? memberRepository.save(existingMember) : existingMember;
    }

    private Member createNewMember(String provider, OAuth2Attribute attribute) {
        Member newMember = Member.builder()
                .provider(provider)
                .providerId(attribute.getProviderId())
                .name(attribute.getUsername())
                .email(attribute.getEmail())
                .role(MemberRole.PREMEMBER)
                .build();
        return memberRepository.save(newMember);
    }
}
