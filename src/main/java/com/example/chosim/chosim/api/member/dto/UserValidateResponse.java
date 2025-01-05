package com.example.chosim.chosim.api.member.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserValidateResponse {

    String username;
    String role;
}
