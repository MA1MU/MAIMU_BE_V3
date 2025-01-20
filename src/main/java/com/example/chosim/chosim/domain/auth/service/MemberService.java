package com.example.chosim.chosim.domain.auth.service;


import com.example.chosim.chosim.api.member.dto.ProfileResponse;
import com.example.chosim.chosim.common.jwt.JwtTokenProvider;
import com.example.chosim.chosim.common.jwt.RefreshToken;
import com.example.chosim.chosim.common.jwt.RefreshTokenRepository;
import com.example.chosim.chosim.common.jwt.Token;
import com.example.chosim.chosim.domain.auth.component.MemberReader;
import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.api.member.dto.ProfileRequest;
import com.example.chosim.chosim.domain.auth.enums.MemberRole;
import com.example.chosim.chosim.domain.auth.repository.MemberRepository;
import com.example.chosim.chosim.common.error.exception.AppException;
import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.group.repository.GroupRepository;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final MaimuRepository maimuRepository;
    private final MemberReader memberReader;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private static final int REFRESH_TOKEN_AGE = 259200;

    //소셜 로그인으로 회원가입 후, 프로필 정보 입력
    @Transactional
    public void joinMember(ProfileRequest request, Long memberId, HttpServletResponse response){
        validateDuplicatemaimuName(request.getNickName());
        log.info("마이무 별명 검증 완료 : {}", request.getNickName());
        Member member = memberReader.findById(memberId);
        member.updateMaimuInfo(request.getMaimuProfile(), request.getBirth(), request.getNickName());
        member.updateRole(MemberRole.MEMBER);

        //토큰 재발급
        Token token = jwtTokenProvider.createToken(memberId, MemberRole.MEMBER.getKey());
        String accessToken = token.getAccessToken();
        response.addHeader("accessToken", accessToken);

        RefreshToken refreshToken = new RefreshToken(member.getId(), token.getRefreshToken());
        refreshToken.updateRefreshToken(token.getRefreshToken());
        refreshTokenRepository.save(refreshToken);

        Cookie cookie = new Cookie("refreshToken", token.getRefreshToken());
        cookie.setPath("/");
        ZonedDateTime seoulTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        ZonedDateTime expirationTime = seoulTime.plusSeconds(REFRESH_TOKEN_AGE);
        cookie.setMaxAge((int) (expirationTime.toEpochSecond() - seoulTime.toEpochSecond()));
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        
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

    private void validateDuplicatemaimuName(String nickName) {
        if (memberRepository.findByNickName(nickName).isPresent()){
            throw new AppException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

    //마이페이지 정보 수정
    @Transactional
    public ProfileResponse updateMemberProfile(ProfileRequest request, Long memberId){
        Member member = memberReader.findById(memberId);
        validateDuplicatemaimuNameInUpdate(request.getNickName(), member);
        log.info("마이무 별명 검증 완료 : {}", request.getNickName());
        member.updateMaimuInfo(request.getMaimuProfile(), request.getBirth(), request.getNickName());
        Member saved = memberRepository.save(member);
        return ProfileResponse.from(saved);
    }

    private void validateDuplicatemaimuNameInUpdate(String nickName, Member member) {
        if (!Objects.equals(nickName, member.getNickName()) && memberRepository.findByNickName(nickName).isPresent()){
            throw new AppException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deleteMember(Long memberId) {
        try {
            doDeleteMember(memberId);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    try {
                        refreshTokenRepository.deleteById(memberId);
                    } catch (Exception e) {
                        log.error("Redis RefreshToken 삭제 실패: {}", e.getMessage());
                        // Redis 삭제 실패 처리 로직
                    }
                }
            });
        } catch (Exception e) {
            log.error("회원 탈퇴 중 오류 발생: {}", e.getMessage());
            throw new AppException(ErrorCode.DELETE_MEMBER_FAILED);
        }
    }

    private void doDeleteMember(Long memberId) {
        Member member = memberRepository.findByIdWithLock(memberId);
        List<Group> groups = groupRepository.findByMemberIdWithLock(memberId);

        groups.forEach(group -> {
            maimuRepository.deleteByGroupId(group.getId());
        });
        groupRepository.deleteByMemberId(memberId);
        memberRepository.delete(member);
    }
}
