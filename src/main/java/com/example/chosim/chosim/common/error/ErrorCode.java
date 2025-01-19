package com.example.chosim.chosim.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    // DTO 에서 발생하는 에러
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "I-001", "입력 값이 잘못되었습니다."),
    // 404 오류 -> 객체를 찾을 수 없는 문제
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "I-201", "해당 Entity를 찾을 수 없습니다."),

    //멤버 관련 오류
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "M-001", "해당 닉네임을 가진 사용자가 존재합니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,"M-002", "해당 회원이 존재하지 않습니다."),
    LOGIN_ID_NOT_FOUND(HttpStatus.NOT_FOUND, "M-003", "해당 아이디가 존재하지 않습니다."),
    DELETE_MEMBER_FAILED(HttpStatus.CONFLICT, "M-004", "회원탈퇴도중 실패했습니다."),
    //로그인 AUTH
    JWT_NOT_EXISTS(HttpStatus.UNAUTHORIZED, "T-001", "Jwt 토큰이 존재하지 않습니다."),
    LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "T-002", "로그인 요청에 실패했습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "T-003", "이미 만료된 토큰입니다."),
    FILTER_EXCEPTION(HttpStatus.UNAUTHORIZED, "T-004", "필터 내부에러 발생"),
    JWT_FORM_ERROR(HttpStatus.UNAUTHORIZED, "T-005", "jwt 형식 에러 발생"),
    REFRESH_TOKEN_NOT_EXIST(HttpStatus.UNAUTHORIZED, "T-006", "해당 리프레시 토큰이 DB에 존재하지 않습니다."),
    REISSUE_FAIL(HttpStatus.UNAUTHORIZED, "T-007", "액세스 토큰 재발급 요청 실패"),
    MEMBER_INFO_NOT_FOUND(HttpStatus.UNAUTHORIZED, "T-008", "로그인된 사용자 정보를 가져올 수 없습니다."),

    //Group
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "G-001", "그룹을 찾을 수 없습니다."),

    //Maimu
    MAIMU_NOT_FOUND(HttpStatus.NOT_FOUND, "M-001", "마이무를 찾을 수 없습니다."),

    //상위 예외 처리
    GENERAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G-001", "예상치 못한 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G-002", "서버 내부 오류가 발생했습니다.");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
