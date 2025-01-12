package com.example.chosim.chosim.common.auth;


import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.auth.enums.MemberRole;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.nio.file.attribute.UserPrincipal;
import java.util.Collection;
import java.util.List;
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
        System.out.println(oAuth2User);

        String provider = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = null;

        if(provider.equals("naver")){
            userInfo = new NaverUserInfo(oAuth2User.getAttributes());
        }

        else if (provider.equals("google")){
            userInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        }
        else{
            userInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        }
        //이전에 provider와 providerId를 통해서 username을 만드는 부분
        //String username = oAuth2Response.getProvider() + oAuth2Response.getProviderId();

        String userNameAttributeKey = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Member member = findExistingMember(userInfo);

        return new CustomOAuth2User(member, attributes,userNameAttributeKey);
    }

    private Member findExistingMember(OAuth2UserInfo userInfo){

        Optional<Member> optionalMember = memberRepository.findByProviderId(userInfo.getProviderId());

        if(optionalMember.isEmpty()){
            Member unregisteredMember = Member.builder()
                    .provider(userInfo.getProvider())
                    .providerId(userInfo.getProviderId())
                    .name(userInfo.getName())
                    .email(userInfo.getEmail())
                    .role(MemberRole.PREMEMBER)
                    .build();

            return memberRepository.save(unregisteredMember);
        }
        return optionalMember.get();
    }
}
