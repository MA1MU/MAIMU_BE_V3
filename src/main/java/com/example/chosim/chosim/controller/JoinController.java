package com.example.chosim.chosim.controller;


import com.example.chosim.chosim.domain.entity.dto.JoinRequest;
import com.example.chosim.chosim.jwt.JWTUtil;
import com.example.chosim.chosim.service.JoinService;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/api/join")
public class JoinController {

    private final JoinService joinService;
    private final JWTUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> joinUser(
            @CookieValue(value="Authorization") Cookie cookie,
            @RequestBody JoinRequest request){
        String username = jwtUtil.getUsername(cookie.getValue());
        joinService.joinUser(request,username);
        return ResponseEntity.status(HttpStatus.OK).body("ROLE_USER로 갱신");
    }

    @GetMapping("/test")
    public String testJoin(
            @CookieValue(value = "Authorization") Cookie cookie
    ){
        return "test Join" + jwtUtil.getRole(cookie.getValue());
    }
}
