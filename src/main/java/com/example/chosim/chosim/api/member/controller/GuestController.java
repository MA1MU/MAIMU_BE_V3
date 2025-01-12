package com.example.chosim.chosim.api.member.controller;

import com.example.chosim.chosim.api.guest.dto.GuestRequest;
import com.example.chosim.chosim.api.guest.dto.GuestResponse;
import com.example.chosim.chosim.domain.group.service.GroupService;
import com.example.chosim.chosim.domain.maimu.service.MaimuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/api/guest")
@RequiredArgsConstructor
public class GuestController {

    private final MaimuService maimuService;
    private final GroupService groupService;

    @GetMapping("/{username}/{groupId}")
    public GuestResponse get(@PathVariable String username, @PathVariable Long groupId){
        com.example.chosim.chosim.api.member.dto.GuestRequest request = com.example.chosim.chosim.api.member.dto.GuestRequest.builder()
                .groupId(groupId)
                .username(username)
                .build();

        return groupService.getForGuest(request);
    }

    @PostMapping("/{groupId}/maimu")
    public void write(@PathVariable Long groupId, @RequestBody @Valid GuestRequest request){
        maimuService.writeMaimu(groupId, request);
    }

}
