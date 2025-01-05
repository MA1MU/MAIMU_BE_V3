package com.example.chosim.chosim.api.group.dto;

import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.domain.group.entity.Group;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class GroupResponse {

    private final Long id;
    private final String groupName;
    private final String groupColor;

    public GroupResponse(Group group){
        this.id = group.getId();
        this.groupName = group.getGroupName();
        this.groupColor = group.getGroupColor();
    }

    @Builder
    public GroupResponse(Long id, String groupName, String groupColor, List<Maimu> maimus){
        this.id = id;
        this.groupName = groupName.substring(0, Math.min(groupName.length(), 20));
        this.groupColor = groupColor;
    }
}