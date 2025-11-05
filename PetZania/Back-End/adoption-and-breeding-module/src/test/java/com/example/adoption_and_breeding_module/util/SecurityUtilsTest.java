package com.example.adoption_and_breeding_module.util;


import com.example.adoption_and_breeding_module.exception.AuthenticatedUserNotFound;
import com.example.adoption_and_breeding_module.model.principal.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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