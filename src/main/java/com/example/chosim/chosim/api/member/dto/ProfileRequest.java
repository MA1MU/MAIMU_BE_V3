package com.example.chosim.chosim.api.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileRequest {

    private String maimuProfile;
    private int year;
    private int month;
    private int date;
    private String nickName;

    public LocalDate getBirth(){
        return LocalDate.of(this.year, this.month, this.date);
    }
}
