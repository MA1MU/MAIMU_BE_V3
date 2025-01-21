package com.example.chosim.chosim.common.auth;

import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.common.error.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomOauth2FailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    public CustomOauth2FailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.LOGIN_FAIL;

        ErrorResponse errorResponse = ErrorResponse.of(errorCode, request);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String errorBody = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(errorBody);
    }
}
