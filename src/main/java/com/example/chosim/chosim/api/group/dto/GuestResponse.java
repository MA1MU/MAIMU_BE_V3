package com.example.chosim.chosim.api.group.dto;

import com.example.chosim.chosim.domain.group.entity.Group;
import lombok.Builder;
import lombok.Getter;

@Getter
public class GuestResponse {

    private final Long id;
    private final String groupName;
    private final String groupColor;
    private final Integer maimuCount;

    public GuestResponse(Group group){
        this.id = group.getId();
        this.groupName = group.getGroupName();
        this.groupColor = group.getGroupColor();
        this.maimuCount = group.getMaimus().size();
    }

    @Builder
    public GuestResponse(Long id, String groupName, String groupColor, Integer maimuCount){
        this.id = id;
        this.groupName = groupName;
        this.groupColor = groupColor;
        this.maimuCount = maimuCount;
    }
}
