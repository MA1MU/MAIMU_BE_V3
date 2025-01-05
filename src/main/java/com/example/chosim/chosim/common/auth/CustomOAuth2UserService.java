package com.example.chosim.chosim.common.auth;


import com.example.chosim.chosim.domain.auth.entity.UserEntity;
import com.example.chosim.chosim.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{

        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println(oAuth2User);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2Response oAuth2Response = null;

        if(registrationId.equals("naver")){
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        }

        else if (registrationId.equals("google")){
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        }
        else{
            oAuth2Response = new KakaoResponse(oAuth2User.getAttributes());
        }
        String username = oAuth2Response.getProvider() + oAuth2Response.getProviderId();


        UserEntity existData = userRepository.findByUsername(username).orElse(null);

        if (existData == null)
        {
            UserEntity newUser = UserEntity.builder()
                    .username(username)
                    .email(oAuth2Response.getEmail())
                    .name(oAuth2Response.getName())
                    .role("ROLE_GUEST")
                    .build();
            userRepository.save(newUser);

//            UserDTO userDTO = new UserDTO();
//            userDTO.setUsername(username);
//            userDTO.setName(oAuth2Response.getName());
//            userDTO.setRole("ROLE_GUEST");

            UserDTO userDTO = UserDTO.builder()
                    .role("ROLE_GUEST")
                    .name(oAuth2Response.getName())
                    .email(oAuth2Response.getEmail())
                    .username(username)
                    .build();

            return new CustomOAuth2User(userDTO);
        }
        else{
//            existData.setName(oAuth2Response.getName());
//            existData.setEmail(oAuth2Response.getEmail());
//            existData.setRole("ROLE_USER");

            userRepository.save(existData);

//            UserDTO userDTO = new UserDTO();
//            userDTO.setUsername(existData.getUsername());
//            userDTO.setName(oAuth2Response.getName());
//            userDTO.setRole(existData.getRole());

            UserDTO userDTO = UserDTO.builder()
                    .role(existData.getRole())
                    .name(oAuth2Response.getName())
                    .email(oAuth2Response.getEmail())
                    .username(existData.getUsername())
                    .build();

            return new CustomOAuth2User(userDTO);
        }
    }
}
