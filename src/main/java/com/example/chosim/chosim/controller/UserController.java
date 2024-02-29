package com.example.chosim.chosim.controller;


import com.example.chosim.chosim.domain.entity.dto.ProfileRequest;
import com.example.chosim.chosim.jwt.JWTUtil;
import com.example.chosim.chosim.service.UserService;
import jakarta.servlet.http.Cookie;
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
            @CookieValue(value="Authorization") Cookie cookie,
            @RequestBody ProfileRequest request){
        String username = jwtUtil.getUsername(cookie.getValue());
        userService.joinUser(request,username);
        return ResponseEntity.status(HttpStatus.OK).body("ROLE_USER로 갱신");
    }

    @PatchMapping("/edit")
    public ResponseEntity<?> updateProfile(
            @CookieValue(value="Authorization") Cookie cookie,
            @RequestBody ProfileRequest request){
        String username = jwtUtil.getUsername(cookie.getValue());
        userService.updateProfile(request,username);
        return ResponseEntity.status(HttpStatus.OK).body("My Profile 갱신");
    }

    @GetMapping("/test")
    public String testJoin(
            @CookieValue(value = "Authorization") Cookie cookie
    ){
        return "test Join" + jwtUtil.getRole(cookie.getValue());
    }
}
