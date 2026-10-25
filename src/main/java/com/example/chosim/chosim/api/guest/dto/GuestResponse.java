package com.example.chosim.chosim.api.guest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;
import lombok.Getter;

@Getter
public class GuestResponse {

    private final Long id;
    private final String groupName;
    private final String groupColor;
    private final Integer maimuCount;

    @Builder
    @JsonCreator
    public GuestResponse(Long id, String groupName, String groupColor, Integer maimuCount){
        this.id = id;
        this.groupName = groupName;
        this.groupColor = groupColor;
        this.maimuCount = maimuCount;
    }
}
