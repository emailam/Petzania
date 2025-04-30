package com.example.registrationmodule.util;

import com.example.registrationmodule.exception.user.AuthenticatedUserNotFound;
import com.example.registrationmodule.model.entity.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AuthenticatedUserNotFound("No authenticated user found");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }
}

