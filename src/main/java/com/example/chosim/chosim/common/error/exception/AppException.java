package com.example.chosim.chosim.common.error.exception;


import com.example.chosim.chosim.common.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AppException extends RuntimeException {

    private ErrorCode errorCode;
}
