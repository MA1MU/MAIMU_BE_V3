package com.example.chosim.chosim.api.group.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class GroupRequest {

    @NotBlank(message = "그룹 이름을 작성해 주세요")
    private String groupName;

    @NotBlank(message = "그룹 색깔을 지정해 주세요")
    private String groupColor;

    @Builder
    public GroupRequest(String groupName, String groupColor){
        this.groupName = groupName;
        this.groupColor = groupColor;
    }
}
