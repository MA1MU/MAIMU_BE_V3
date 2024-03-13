package com.example.chosim.chosim.dto.response.user;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoResponse {

    private String maimuProfile;
    private int year;
    private int month;
    private int date;
    private String role;
    private String maimuName;
}
