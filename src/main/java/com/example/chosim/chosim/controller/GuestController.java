package com.example.chosim.chosim.controller;

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

    @GetMapping("/guest/{groupId}")
    public GuestResponse get(@PathVariable Long groupId){
        return groupService.getForGuest(groupId);
    }

    @PostMapping("/guest/{groupId}/maimu")
    public void write(@PathVariable Long groupId, @RequestBody @Valid MaimuCreate request){
        maimuService.writeMaimu(groupId, request);
    }


}
