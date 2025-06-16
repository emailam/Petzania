package com.example.adoption_and_breeding_module.util;

import com.example.adoption_and_breeding_module.exception.AuthenticatedUserNotFound;
import com.example.adoption_and_breeding_module.model.principal.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AuthenticatedUserNotFound("No authenticated user found");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }
}
