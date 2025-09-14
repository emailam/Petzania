package com.example.friends.and.chats.module.util;


import com.example.friends.and.chats.module.exception.user.AuthenticatedUserNotFound;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_ReturnsUserPrincipal() {
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertSame(userPrincipal, SecurityUtils.getCurrentUser());
    }

    @Test
    void getCurrentUser_ThrowsIfNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThrows(AuthenticatedUserNotFound.class, SecurityUtils::getCurrentUser);
    }

    @Test
    void getCurrentUser_ThrowsIfPrincipalIsNotUserPrincipal() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("not-a-user-principal");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(AuthenticatedUserNotFound.class, SecurityUtils::getCurrentUser);
    }
}