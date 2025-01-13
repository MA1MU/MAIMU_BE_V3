package com.example.chosim.chosim.api.guest.controller;

import com.example.chosim.chosim.domain.guest.service.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/api/guest")
@RequiredArgsConstructor
@Tag(name = "발신자 관련 API", description = "링크를 받은 발신자는 마이무를 작성할 수 있다.")
public class GuestController {

    private final GuestService guestService;

//    @GetMapping("/{memberId}/{groupId}/all")
//    @Operation(summary = "게스트가 사용자 그룹에 진입하기", description = "게스트가 특정 사용자의 그룹에 존재하는 마이무의 갯수를 볼 수 있음")
//    public ResponseEntity<>
//
//
//    @PostMapping("/{memberId}/{groupdId}")

}
