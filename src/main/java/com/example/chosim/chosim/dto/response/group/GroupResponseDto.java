package com.example.chosim.chosim.dto.response.group;

import com.example.chosim.chosim.domain.Maimu;
import com.example.chosim.chosim.domain.group.Group;
import lombok.Builder;

public class GroupResponseDto {
    private final Long id;
    private final String groupName;
    private final String groupColor;

    public GroupResponseDto(Group group){
        this.id = group.getId();
        this.groupName = group.getGroupName();
        this.groupColor = group.getGroupColor();
    }

    @Builder
    public GroupResponseDto(Long id, String groupName, String groupColor){
        this.id = id;
        this.groupName = groupName.substring(0, Math.min(groupName.length(), 20));
        this.groupColor = groupColor;
    }
}
