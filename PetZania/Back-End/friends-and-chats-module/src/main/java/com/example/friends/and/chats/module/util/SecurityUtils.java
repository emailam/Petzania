package com.example.friends.and.chats.module.util;

import com.example.friends.and.chats.module.exception.user.AuthenticatedUserNotFound;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
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
