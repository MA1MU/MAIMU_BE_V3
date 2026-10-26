package com.example.chosim.chosim.common.auth;

import com.example.chosim.chosim.common.error.ErrorCode;
import com.example.chosim.chosim.common.error.exception.AppException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentMember {
    private CurrentMember() { }

    public static void requireOwner(Long ownerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new AppException(ErrorCode.MEMBER_INFO_NOT_FOUND);
        }
        if (!auth.getPrincipal().equals(ownerId)) {
            throw new AppException(ErrorCode.DELETE_FORBIDDEN);
        }
    }
}
