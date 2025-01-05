package com.example.chosim.chosim.api.member.controller;


import com.example.chosim.chosim.domain.entity.dto.ProfileRequest;
import com.example.chosim.chosim.api.member.dto.UserInfoResponse;
import com.example.chosim.chosim.api.member.dto.UserValidateResponse;
import com.example.chosim.chosim.common.jwt.JWTUtil;
import com.example.chosim.chosim.domain.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JWTUtil jwtUtil;

    @PostMapping("/join")
    public ResponseEntity<?> joinUser(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody ProfileRequest request){
        String username = jwtUtil.getUsername(accessToken);
        userService.joinUser(request,username);
        return ResponseEntity.status(HttpStatus.OK).body("ROLE_USER로 갱신");
    }

    @PatchMapping("/edit")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody ProfileRequest request){
        String username = jwtUtil.getUsername(accessToken);
        userService.updateProfile(request,username);
        return ResponseEntity.status(HttpStatus.OK).body("My Profile 갱신");
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
    public UserInfoResponse getUserInfo(
            @RequestHeader("Authorization") String accessToken
    ){
        String username = jwtUtil.getUsername(accessToken);
        return userService.findUserInfobyUsername(username);
    }

    @DeleteMapping("/logout")
    public ResponseEntity<?> userLogout(
            @RequestHeader("Authorization") String accessToken
    ){
        String username = jwtUtil.getUsername(accessToken);
        userService.deleteUser(username);
        return ResponseEntity.status(HttpStatus.OK).body("회원 탈퇴 성공");
    }



//    @GetMapping("/test")
//    public String testJoin(
//            @CookieValue(value = "Authorization") Cookie cookie
//    ){
//        return "test Join" + jwtUtil.getRole(cookie.getValue());
//    }
}
