package com.example.chosim.chosim.controller;

import com.example.chosim.chosim.dto.request.group.GroupCreate;
import com.example.chosim.chosim.dto.request.group.GroupEdit;
import com.example.chosim.chosim.dto.response.group.GroupListResponse;
import com.example.chosim.chosim.dto.response.group.GroupResponse;
import com.example.chosim.chosim.dto.response.group.GroupResponseDto;
import com.example.chosim.chosim.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/api/join/{username}")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/group")
    public void group(@PathVariable String username, @RequestBody @Valid GroupCreate groupCreate){
        groupService.createGroup(username, groupCreate);
    }

    @GetMapping("/group")
    public GroupListResponse groupList(@PathVariable String username){
        List<GroupResponseDto> all = groupService.getList(username);
        return new GroupListResponse(all);
    }

    @GetMapping(value = "/group/{groupId}", produces = "application/json")
    public GroupResponse get(@PathVariable Long groupId){
        return groupService.get(groupId);
    }


    @PatchMapping("/group/{groupId}")
    public void edit(@PathVariable Long groupId, @RequestBody @Valid GroupEdit groupEdit){
        groupService.edit(groupId, groupEdit);
    }

    @DeleteMapping("/group/{groupId}")
    public void delete(@PathVariable Long groupId){
        groupService.delete(groupId);
    }

}

