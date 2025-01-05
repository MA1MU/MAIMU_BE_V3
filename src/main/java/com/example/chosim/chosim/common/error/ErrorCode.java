package com.example.chosim.chosim.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "해당 닉네임을 가진 사용자가 존재합니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 회원이 존재하지 않습니다."),
    JWT_NOT_EXISTS(HttpStatus.UNAUTHORIZED, "Jwt 토큰이 존재하지 않습니다."),
    LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "로그인 요청에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

}
