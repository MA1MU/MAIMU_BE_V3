package com.example.chosim.chosim.controller;

import com.example.chosim.chosim.dto.request.GuestRequest;
import com.example.chosim.chosim.dto.request.maimu.MaimuCreate;
import com.example.chosim.chosim.dto.response.group.GroupResponse;
import com.example.chosim.chosim.dto.response.group.GuestResponse;
import com.example.chosim.chosim.repository.maimu.MaimuRepository;
import com.example.chosim.chosim.service.GroupService;
import com.example.chosim.chosim.service.MaimuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GuestController {

    private final MaimuService maimuService;
    private final GroupService groupService;

    @GetMapping("/guest/{username}/{groupId}")
    public GuestResponse get(@PathVariable String username, @PathVariable Long groupId){
        GuestRequest request = GuestRequest.builder()
                .groupId(groupId)
                .username(username)
                .build();

        return groupService.getForGuest(request);
    }

    @PostMapping("/guest/{groupId}/maimu")
    public void write(@PathVariable Long groupId, @RequestBody @Valid MaimuCreate request){
        maimuService.writeMaimu(groupId, request);
    }

}
