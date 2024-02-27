package com.example.chosim.chosim.dto.request.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Service;

@Setter
@Getter
@ToString
public class GroupCreate {

    @NotBlank(message = "write group name.")
    private String groupName;

    @NotBlank(message = "write group color.")
    private String groupColor;

    @Builder
    public GroupCreate(String groupName, String groupColor){
        this.groupName = groupName;
        this.groupColor = groupColor;
    }
}
