package com.example.chosim.chosim.api.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LogoutRequest {

    @NotNull
    String accessToken;
}
