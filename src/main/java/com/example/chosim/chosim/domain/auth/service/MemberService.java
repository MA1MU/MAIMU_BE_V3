package com.example.chosim.chosim.domain.auth.service;


import com.example.chosim.chosim.api.member.dto.ProfileResponse;
import com.example.chosim.chosim.domain.auth.component.MemberReader;
import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.api.member.dto.ProfileRequest;
import com.example.chosim.chosim.domain.auth.enums.MemberRole;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import com.example.chosim.chosim.common.error.exception.AppException;
import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final MaimuRepository maimuRepository;
    private final MemberReader memberReader;
    
    //소셜 로그인으로 회원가입 후, 프로필 정보 입력
    @Transactional
    public void joinMember(ProfileRequest request, Long memberId){
        validateDuplicatemaimuName(request.getNickName());
        log.info("마이무 별명 검증 완료 : {}", request.getNickName());
        Member member = memberReader.findById(memberId);
        member.updateMaimuInfo(request.getMaimuProfile(), request.getBirth(), request.getNickName());
        member.updateRole(MemberRole.MEMBER);
        memberRepository.save(member);
    }

    //마이페이지 정보 가져오기
    public ProfileResponse findMemberProfile(Long memberId){
        Member member = memberReader.findById(memberId);
        ProfileResponse profileResponse = ProfileResponse.builder()
                .maimuProfile(member.getMaimuProfile())
                .birth(member.getBirth())
                .nickName(member.getNickName())
                .build();
        return profileResponse;
    }

    //마이페이지 정보 수정
    @Transactional
    public ProfileResponse updateMemberProfile(ProfileRequest request, Long memberId){
        validateDuplicatemaimuName(request.getNickName());
        log.info("마이무 별명 검증 완료 : {}", request.getNickName());
        Member member = memberReader.findById(memberId);
        member.updateMaimuInfo(request.getMaimuProfile(), request.getBirth(), request.getNickName());
        Member saved = memberRepository.save(member);
        return ProfileResponse.from(saved);
    }

    private void validateDuplicatemaimuName(String nickName) {
        if (memberRepository.findByNickName(nickName).isPresent()){
            throw new AppException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

    //회원 탈퇴 후 정보 삭제
//    @Transactional
//    public void deleteMember(Long memberId){
//        Member member = memberReader.findById(memberId);
//        List<Group> groupList =  groupRepository.findByUserEntity_IdOrderByIdAsc(userId);
//
//        for(Group group : groupList){
//           Long groupId = group.getId();
//           maimuRepository.deleteAllInBatch(maimuRepository.findByGroup_IdOrderByIdAsc(groupId));
//        }
//        groupRepository.deleteAllInBatch(groupList);
//        memberRepository.delete(user);
//    }
}
