package com.example.chosim.chosim.common.error.response;

import com.example.chosim.chosim.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public record ErrorResponse (
        String code,
        String message,
        String method,
        String requestURI
){

    public static ErrorResponse of(ErrorCode errorCode, HttpServletRequest request){
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                request.getMethod(),
                request.getRequestURI()
        );
    }

    public static ErrorResponse of(HttpServletRequest request, ErrorCode errorCode, final String errorMessage) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorMessage,
                request.getMethod(),
                request.getRequestURI()
        );
    }

}
