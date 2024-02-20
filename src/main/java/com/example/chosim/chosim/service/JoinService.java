package com.example.chosim.chosim.service;


import com.example.chosim.chosim.domain.entity.UserEntity;
import com.example.chosim.chosim.domain.entity.dto.JoinRequest;
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
public class JoinService {

    private final UserRepository userRepository;

    @Transactional
    public void joinUser(JoinRequest request,String username){

        validateDuplicatemaimuName(request.getMaimuName());
        log.info("마이무 별명 검증 완료 : {}", request.getMaimuName());

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(()-> new AppException(ErrorCode.MEMBER_NOT_FOUND));
        user.initMaimuInfo(request.getMaimuProfile(), request.getBirth(), request.getMaimuName());
        user.authUser("ROLE_USER");

        userRepository.save(user);

    }

    private void validateDuplicatemaimuName(String maimuName) {

        if (userRepository.findBymaimuName(maimuName).isPresent()){
            throw new AppException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

}
