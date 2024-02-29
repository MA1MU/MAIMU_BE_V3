package com.example.chosim.chosim.service;


import com.example.chosim.chosim.domain.entity.UserEntity;
import com.example.chosim.chosim.domain.entity.dto.ProfileRequest;
import com.example.chosim.chosim.domain.entity.repository.UserRepository;
import com.example.chosim.chosim.exception.AppException;
import com.example.chosim.chosim.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void joinUser(ProfileRequest request, String username){

        validateDuplicatemaimuName(request.getMaimuName());
        log.info("마이무 별명 검증 완료 : {}", request.getMaimuName());

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(()-> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        user.updateMaimuInfo(request.getMaimuProfile(), request.getBirth(), request.getMaimuName());
        user.authUser("ROLE_USER");

        userRepository.save(user);

    }

    @Transactional
    public void updateProfile(ProfileRequest request, String username){

        validateDuplicatemaimuName(request.getMaimuName());
        log.info("마이무 별명 검증 완료 : {}", request.getMaimuName());

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(()-> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        user.updateMaimuInfo(request.getMaimuProfile(), request.getBirth(), request.getMaimuName());
        userRepository.save(user);

    }

    private void validateDuplicatemaimuName(String maimuName) {

        if (userRepository.findBymaimuName(maimuName).isPresent()){
            throw new AppException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

}
