package com.example.chosim.chosim.domain.auth.service;


import com.example.chosim.chosim.domain.auth.entity.UserEntity;
import com.example.chosim.chosim.domain.entity.dto.ProfileRequest;
import com.example.chosim.chosim.domain.auth.repository.UserRepository;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.api.member.dto.UserInfoResponse;
import com.example.chosim.chosim.common.error.exception.AppException;
import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MaimuRepository maimuRepository;


    public UserInfoResponse findUserInfobyUsername(String username){
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        LocalDate userBirth = user.getBirth();
        UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                .maimuProfile(user.getMaimuProfile())
                .year(userBirth.getYear())
                .month(userBirth.getMonthValue())
                .date(userBirth.getDayOfMonth())
                .maimuName(user.getMaimuName())
                .role(user.getRole())
                .build();
        return userInfoResponse;
    }

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

    public void deleteUser(String username){
        UserEntity user = userRepository.findByUsername(username).orElseThrow();

        Long userId = user.getId();


        List<Group> groupList =  groupRepository.findByUserEntity_IdOrderByIdAsc(userId);

        for(Group group : groupList){
           Long groupId = group.getId();
           maimuRepository.deleteAllInBatch(maimuRepository.findByGroup_IdOrderByIdAsc(groupId));
        }
        groupRepository.deleteAllInBatch(groupList);
        userRepository.delete(user);
    }
}
