package com.example.chosim.chosim.controller;

import com.example.chosim.chosim.dto.request.maimu.MaimuCreate;
import com.example.chosim.chosim.dto.request.maimu.MaimuDelete;
import com.example.chosim.chosim.dto.request.maimu.MaimuSearch;
import com.example.chosim.chosim.dto.response.maimu.MaimuListResponse;
import com.example.chosim.chosim.dto.response.maimu.MaimuResponse;
import com.example.chosim.chosim.dto.response.maimu.MaimuResponseDto;
import com.example.chosim.chosim.service.MaimuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MaimuController {

    private final MaimuService maimuService;


    //마이무 1개 내용 보기
    @GetMapping("/maimu/{maimuId}")
    public MaimuResponse get(@PathVariable Long maimuId){
        return maimuService.get(maimuId);
    }

    //페이징 처리한 마이무 여러개 보기
    @GetMapping("/{groupId}/maimu")
    public MaimuListResponse getList(@PathVariable Long groupId){
        List<MaimuResponseDto> all = maimuService.getList(groupId);
        return new MaimuListResponse(all);
    }

    @DeleteMapping("/maimu/{maimuId}/delete")
    public void delete(@PathVariable Long maimuId){
        maimuService.deleteMaimu(maimuId);
    }

}