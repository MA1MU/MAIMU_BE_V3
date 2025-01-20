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
    private final Long unreadMaimuCount;

    public GroupResponse(Long id, String groupName, String groupColor, Long unreadMaimuCount) {
        this.id = id;
        this.groupName = groupName.substring(0, Math.min(groupName.length(), 20));
        this.groupColor = groupColor;
        this.unreadMaimuCount = unreadMaimuCount != null ? unreadMaimuCount : 0L;
    }

    public static GroupResponse from(Group group) {
        return new GroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getGroupColor(),
                0L
        );
    }
}