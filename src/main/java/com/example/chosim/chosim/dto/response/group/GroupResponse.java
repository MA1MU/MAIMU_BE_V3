package com.example.chosim.chosim.dto.response.group;

import com.example.chosim.chosim.domain.Maimu;
import com.example.chosim.chosim.domain.group.Group;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class GroupResponse {

    private final Long id;
    private final String groupName;
    private final String groupColor;
    private final List<Maimu> maimus;

    public GroupResponse(Group group){
        this.id = group.getId();
        this.groupName = group.getGroupName();
        this.groupColor = group.getGroupColor();
        this.maimus = group.getMaimus();
    }

    @Builder
    public GroupResponse(Long id, String groupName, String groupColor, List<Maimu> maimus){
        this.id = id;
        this.groupName = groupName.substring(0, Math.min(groupName.length(), 20));
        this.groupColor = groupColor;
        this.maimus = maimus;
    }
}