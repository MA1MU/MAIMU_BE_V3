package com.example.chosim.chosim.domain.entity.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class JoinRequest {

    private int maimuProfile;
    private int year;
    private int month;
    private int date;

    private String maimuName;

    public LocalDate getBirth(){
        return LocalDate.of(this.year, this.month, this.date);
    }
}
