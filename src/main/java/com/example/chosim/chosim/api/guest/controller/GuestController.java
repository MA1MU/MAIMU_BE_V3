package com.example.chosim.chosim.api.guest.controller;

import com.example.chosim.chosim.api.guest.dto.GuestRequest;
import com.example.chosim.chosim.api.guest.dto.GuestResponse;
import com.example.chosim.chosim.domain.group.entity.Group;
import com.example.chosim.chosim.domain.guest.service.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/api/guest")
@RequiredArgsConstructor
@Tag(name = "발신자 관련 API", description = "링크를 받은 발신자는 마이무를 작성할 수 있다.")
public class GuestController {

    private final GuestService guestService;

    @GetMapping("/{memberId}/{groupId}/all")
    @Operation(summary = "게스트가 사용자 그룹에 진입하기", description = "게스트가 특정 사용자의 그룹에 존재하는 마이무의 갯수를 볼 수 있음")
    public ResponseEntity<GuestResponse> enterGroup(@PathVariable Long memberId, @PathVariable Long groupId){
        return ResponseEntity.status(HttpStatus.OK).body(guestService.enterGroup(groupId));
    }

    @PostMapping("/{memberId}/{groupId}/add")
    @Operation(summary = "게스트가 마이무 작성", description= "게스트가 특정 그룹 진입 후 마이무 작성 이후 저장")
    public ResponseEntity<Void> addMaimu(@PathVariable Long memberId, @PathVariable Long groupId,
                                         @Valid @RequestBody GuestRequest guestRequest){
        guestService.addMaimu(groupId, guestRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
