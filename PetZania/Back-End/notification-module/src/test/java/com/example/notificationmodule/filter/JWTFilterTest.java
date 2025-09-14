package com.example.notificationmodule.filter;

import com.example.notificationmodule.model.principal.AdminPrincipal;
import com.example.notificationmodule.model.principal.UserPrincipal;
import com.example.notificationmodule.service.impl.JWTService;
import com.example.notificationmodule.service.impl.MyUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JWTFilterTest {

    @Mock JWTService jwtService;
    @Mock ApplicationContext applicationContext;
    @Mock MyUserDetailsService userDetailsService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    JWTFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new JWTFilter(jwtService, applicationContext);
        // Must clear SecurityContext every test!
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateAsUser() throws Exception {
        String token = "valid-user-token";
        String email = "user@mail.com";
        String role = "ROLE_USER";

        // Given request with Bearer header
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(role);

        // User principal returned from service
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(applicationContext.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByEmail(email)).thenReturn(userPrincipal);
        when(jwtService.validateTokenForUser(token, userPrincipal)).thenReturn(true);
        when(userPrincipal.getAuthorities()).thenCallRealMethod();
        doNothing().when(userPrincipal).setGrantedAuthority(any());

        // When: filter runs
        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userPrincipal, authentication.getPrincipal());

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateAsAdmin() throws Exception {
        String token = "valid-admin-token";
        String email = "admin@mail.com";
        String role = "ROLE_ADMIN";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(role);

        AdminPrincipal adminPrincipal = mock(AdminPrincipal.class);
        when(applicationContext.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadAdminByUsername(email)).thenReturn(adminPrincipal);
        doNothing().when(adminPrincipal).setGrantedAuthority(any());
        when(jwtService.validateTokenForAdmin(token, adminPrincipal)).thenReturn(true);
        when(adminPrincipal.getAuthorities()).thenCallRealMethod();

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(adminPrincipal, authentication.getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenHeaderMissing() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWithInvalidHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("invalidheader");

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateIfTokenInvalid() throws Exception {
        String token = "token";
        String email = "user@mail.com";
        String role = "ROLE_USER";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(role);

        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(applicationContext.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByEmail(email)).thenReturn(userPrincipal);
        doNothing().when(userPrincipal).setGrantedAuthority(any());
        when(jwtService.validateTokenForUser(token, userPrincipal)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}