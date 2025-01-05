package com.example.chosim.chosim.api.maimu.controller;

import com.example.chosim.chosim.api.group.dto.GroupResponse;
import com.example.chosim.chosim.api.maimu.dto.MaimuListResponse;
import com.example.chosim.chosim.api.maimu.dto.MaimuResponse;
import com.example.chosim.chosim.domain.group.service.GroupService;
import com.example.chosim.chosim.domain.maimu.service.MaimuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/api/maimu")
@RequiredArgsConstructor
public class MaimuController {

    private final MaimuService maimuService;
    private final GroupService groupService;

    //마이무 1개 내용 보기
    @GetMapping("/{maimuId}")
    public MaimuResponse get(@PathVariable Long maimuId){
        return maimuService.get(maimuId);
    }

    //마이무 여러개 보기
    @GetMapping("/{groupId}/all")
    public MaimuListResponse getList(@PathVariable Long groupId){
        List<MaimuResponse> all = maimuService.getList(groupId);
        GroupResponse response = groupService.get(groupId);
        return new MaimuListResponse(response, all);
    }

    @DeleteMapping("/{maimuId}/delete")
    public void delete(@PathVariable Long maimuId){
        maimuService.deleteMaimu(maimuId);
    }

}