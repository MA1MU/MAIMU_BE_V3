package com.example.chosim.chosim.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class GuestRequest {

    private Long groupId;

    private String username;

    @Builder
    public GuestRequest(Long groupId, String username){
        this.groupId = groupId;
        this.username = username;
    }
}
