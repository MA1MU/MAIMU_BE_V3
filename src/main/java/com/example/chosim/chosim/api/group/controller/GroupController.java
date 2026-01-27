package com.example.chosim.chosim.api.group.controller;

import com.example.chosim.chosim.api.group.dto.GroupRequest;
import com.example.chosim.chosim.api.group.dto.GroupResponse;
import com.example.chosim.chosim.common.dto.ResponseDTO;
import com.example.chosim.chosim.domain.auth.service.MemberService;
import com.example.chosim.chosim.domain.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/api/group")
@RequiredArgsConstructor
@Tag(name = "그룹 관련 API", description = "사용자는 자신의 사물함 페이지에서 그룹에 관한 기능에 접근할 수 있다.")
public class GroupController {

    private final GroupService groupService;
    private final MemberService memberService;

    @GetMapping("/all")
    @Operation(summary = "모든 그룹 정보 가져오기", description = "로그인한 후 나의 사물함 페이지에서 내가 가진 모든 그룹 정보를 가져온다.")
    public ResponseEntity<ResponseDTO> getGroupList(@AuthenticationPrincipal Long memberId){
        List<GroupResponse> groupResponseList = groupService.getList(memberId);
        String maimuProfile = memberService.findMemberMaimuProfile(memberId);
        return ResponseEntity.status(HttpStatus.OK).body(new ResponseDTO(maimuProfile, groupResponseList));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "개별 그룹 정보 가져오기", description = "로그인한 후 나의 사물함 페이지에 존재하는 개별 그룹에 대한 정보를 가져온다.")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long groupId){
        return ResponseEntity.status(HttpStatus.OK).body(groupService.getGroup(groupId));
    }

    @PostMapping
    @Operation(summary = "새로운 그룹 추가하기", description = "로그인한 후 나의 사물함 페이지에 새로운 그룹을 추가한다.")
    public ResponseEntity<GroupResponse> createGroup(@AuthenticationPrincipal Long memberId,
                                                     @Valid @RequestBody GroupRequest groupRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(memberId, groupRequest));
    }

    @PatchMapping("/{groupId}")
    @Operation(summary = "기존 그룹 수정하기", description = "나의 사물함 페이지에 있는 그룹 하나를 수정한다.")
    public ResponseEntity<GroupResponse> editGroup(@PathVariable Long groupId, @Valid @RequestBody GroupRequest groupRequest){
        return ResponseEntity.status(HttpStatus.OK).body(groupService.editGroup(groupId, groupRequest));
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "기존 그룹 삭제하기", description = "나의 사물함 페이지에 있는 그룹 하나를 삭제한다.")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId){
        groupService.deleteGroup(groupId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/{groupId}/invitation")
    @Operation(summary = "초대 링크 토큰 생성", description = "그룹 공유를 위한 토큰을 생성하고 Redis에 저장합니다.")
    public ResponseEntity<String> createInviteLink(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long groupId) {

        String token = groupService.createInvitation(groupId, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }
}

