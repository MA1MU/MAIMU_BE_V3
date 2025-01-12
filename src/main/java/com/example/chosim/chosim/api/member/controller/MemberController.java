package com.example.chosim.chosim.api.member.controller;


import com.example.chosim.chosim.api.member.dto.ProfileRequest;
import com.example.chosim.chosim.api.member.dto.ProfileResponse;
import com.example.chosim.chosim.api.member.dto.UserValidateResponse;
import com.example.chosim.chosim.common.jwt.JWTUtil;
import com.example.chosim.chosim.domain.auth.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/member")
@Tag(name = "회원 관련 API", description = "회원가입 한 사용자 정보를 조회 및 변경할 수 있음")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/join")
    @Operation(summary = "프로필 입력 회원가입", description = "소셜 로그인 후 프로필 정보를 입력합니다.")
    public ResponseEntity<Void> joinMember(@AuthenticationPrincipal Long memberId, @RequestBody ProfileRequest request){
        memberService.joinMember(request, memberId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping("/edit")
    @Operation(summary = "프로필 수정", description = "프로필 정보를 수정하고 저장합니다.")
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal Long memberId, @RequestBody ProfileRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(memberService.updateMemberProfile(request, memberId));
    }

    @GetMapping("/validate")
    public UserValidateResponse validateUser(
            @RequestHeader("Authorization") String accessToken
    ){
        String role = jwtUtil.getRole(accessToken);
        String username = jwtUtil.getUsername(accessToken);
        return  UserValidateResponse.builder()
                .username(username)
                .role(role)
                .build();
    }
    @GetMapping("/info")
    public MemberInfoResponse getUserInfo(
            @RequestHeader("Authorization") String accessToken
    ){
        String username = jwtUtil.getUsername(accessToken);
        return memberService.findUserInfobyUsername(username);
    }

    @DeleteMapping("/logout")
    public ResponseEntity<?> userLogout(
            @RequestHeader("Authorization") String accessToken
    ){
        String username = jwtUtil.getUsername(accessToken);
        memberService.deleteUser(username);
        return ResponseEntity.status(HttpStatus.OK).body("회원 탈퇴 성공");
    }

}
