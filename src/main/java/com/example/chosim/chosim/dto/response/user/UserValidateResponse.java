package com.example.chosim.chosim.dto.response.user;


import com.example.chosim.chosim.domain.Maimu;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserValidateResponse {

    String username;
    String role;
}
