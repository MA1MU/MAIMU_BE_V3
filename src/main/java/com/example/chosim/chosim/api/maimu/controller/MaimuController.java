package com.example.chosim.chosim.api.maimu.controller;

import com.example.chosim.chosim.api.group.dto.GroupResponse;
import com.example.chosim.chosim.api.maimu.dto.MaimuFavoriteResponse;
import com.example.chosim.chosim.api.maimu.dto.MaimuResponse;
import com.example.chosim.chosim.api.maimu.dto.PageMaimuResponse;
import com.example.chosim.chosim.api.member.dto.ProfileRequest;
import com.example.chosim.chosim.api.member.dto.ProfileResponse;
import com.example.chosim.chosim.common.dto.ResponseDTO;
import com.example.chosim.chosim.domain.group.service.GroupService;
import com.example.chosim.chosim.domain.maimu.service.MaimuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/api/maimu")
@RequiredArgsConstructor
@Tag(name = "마이무 관련 API", description = "사용자는 자신의 마이무 페이지에서 마이무에 관한 기능에 접근할 수 있다.")
public class MaimuController {

    private final MaimuService maimuService;

    //마이무 여러개 보기
    @GetMapping("/{groupId}/all")
    @Operation(summary = "모든 마이무정보 페이지 처리로 보기", description = "내 그룹과 관련된 마이무 갯수를 페이지 처리로 한 번에 18개만 가져올 수 있게 합니다.")
    public ResponseEntity<PageMaimuResponse> getList(@PathVariable Long groupId, @RequestParam(defaultValue = "0") int page){
        Page<MaimuResponse> maimuPage = maimuService.getList(groupId, page);
        PageMaimuResponse response = new PageMaimuResponse(maimuPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    //마이무 1개 내용 보기
    @GetMapping("/{maimuId}")
    @Operation(summary = "마이무 1개 정보 보기", description = "마이무 1개의 상세 정보를 조회하며, 조회 시 읽음 상태로 변경됩니다.")
    public ResponseEntity<MaimuResponse> getSingleMaimuInfo(@PathVariable Long maimuId){
        MaimuResponse maimuReponse= maimuService.getMaimu(maimuId);
        return ResponseEntity.status(HttpStatus.OK).body(maimuReponse);
    }

    @DeleteMapping("/{maimuId}/delete")
    @Operation(summary = "마이무 삭제하기", description = "마이무 1개를 삭제합니다.")
    public ResponseEntity<Void> delete(@PathVariable Long maimuId){
        maimuService.deleteMaimu(maimuId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping("/{maimuId}/favorite")
    @Operation(summary = "마이무 즐겨찾기", description = "해당 마이무의 즐겨찾기 옵션을 바꾸고 갱신된 마이무 리스트를 반환합니다.")
    public ResponseEntity<MaimuFavoriteResponse> toggleFavorite(@PathVariable Long maimuId){

        return ResponseEntity.status(HttpStatus.OK).body(maimuService.updateFavorite(maimuId));
    }
}