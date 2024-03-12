package com.example.chosim.chosim.controller;

import com.example.chosim.chosim.dto.request.group.GroupCreate;
import com.example.chosim.chosim.dto.request.group.GroupEdit;
import com.example.chosim.chosim.dto.response.group.GroupListResponse;
import com.example.chosim.chosim.dto.response.group.GroupResponse;
import com.example.chosim.chosim.jwt.JWTUtil;
import com.example.chosim.chosim.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/api/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final JWTUtil jwtUtil;

//    @PostMapping("/{username}")
//    public void group(@PathVariable String username, @RequestBody @Valid GroupCreate groupCreate){
//        groupService.createGroup(username, groupCreate);
//    }
    @PostMapping
    public void group(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody @Valid GroupCreate groupCreate){
        groupService.createGroup(jwtUtil.getUsername(accessToken) , groupCreate);
    }


//    @GetMapping("/{username}/all")
//    public GroupListResponse groupList(@PathVariable String username){
//        List<GroupResponse> all = groupService.getList(username);
//        return new GroupListResponse(all);
//    }
    @GetMapping("/all")
    public GroupListResponse groupList(
            @RequestHeader("Authorization") String accessToken){
        List<GroupResponse> all = groupService.getList(jwtUtil.getUsername(accessToken));
        return new GroupListResponse(all);
    }

    @GetMapping(value = "/{groupId}", produces = "application/json")
    public GroupResponse get(@PathVariable Long groupId){
        return groupService.get(groupId);
    }


    @PatchMapping("/{groupId}")
    public void edit(@PathVariable Long groupId, @RequestBody @Valid GroupEdit groupEdit){
        groupService.edit(groupId, groupEdit);
    }

    @DeleteMapping("/{groupId}")
    public void delete(@PathVariable Long groupId){
        groupService.delete(groupId);
    }

}

