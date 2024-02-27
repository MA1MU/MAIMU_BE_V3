package com.example.chosim.chosim.domain.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinRequest {

    private String maimuProfile;
    private int year;
    private int month;
    private int date;

    private String maimuName;

    public LocalDate getBirth(){
        return LocalDate.of(this.year, this.month, this.date);
    }
}
