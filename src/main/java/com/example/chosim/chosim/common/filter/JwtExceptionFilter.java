package com.example.chosim.chosim.common.filter;

import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.common.error.exception.FilterAuthenticationException;
import com.example.chosim.chosim.common.error.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtExceptionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.TOKEN_EXPIRED, request);
            setErrorResponse(response, errorResponse);
        } catch (MalformedJwtException | SignatureException e) {
            ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.JWT_FORM_ERROR, request);
            setErrorResponse(response, errorResponse);
        } catch (FilterAuthenticationException e) {
            ErrorResponse errorResponse = ErrorResponse.of(request, ErrorCode.FILTER_EXCEPTION, e.getMessage());
            setErrorResponse(response, errorResponse);
        }
    }

    private void setErrorResponse(HttpServletResponse response, ErrorResponse errorResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
