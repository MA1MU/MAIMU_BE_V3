package com.example.chosim.chosim.api.member.dto;

import com.example.chosim.chosim.domain.auth.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class ProfileResponse {

    private String maimuProfile;
    private LocalDate birth;
    private String nickName;

    public static ProfileResponse from(Member member) {
        return ProfileResponse.builder()
                .maimuProfile(member.getMaimuProfile())
                .birth(member.getBirth())
                .nickName(member.getNickName())
                .build();
    }
}
