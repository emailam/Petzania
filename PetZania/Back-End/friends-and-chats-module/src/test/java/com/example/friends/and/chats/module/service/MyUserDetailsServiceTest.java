package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.exception.admin.AdminNotFound;
import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.entity.Admin;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.principal.AdminPrincipal;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.repository.AdminRepository;
import com.example.friends.and.chats.module.repository.UserRepository;
import com.example.friends.and.chats.module.service.impl.MyUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MyUserDetailsServiceTest {
    private UserRepository userRepository;
    private AdminRepository adminRepository;
    private MyUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        adminRepository = mock(AdminRepository.class);
        userDetailsService = new MyUserDetailsService(userRepository, adminRepository);
    }

    @Test
    void loadUserByUsername_FindsUser() {
        User user = User.builder().email("x@y.com").build();
        when(userRepository.findByEmailIgnoreCase(eq("x@y.com"))).thenReturn(Optional.of(user));

        assertTrue(userDetailsService.loadUserByUsername("x@y.com") instanceof UserPrincipal);
    }

    @Test
    void loadUserByUsername_FindsAdmin() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenThrow(new UserNotFound("not"));
        Admin admin = Admin.builder().username("admin1").build();
        when(adminRepository.findByUsernameIgnoreCase(eq("admin1"))).thenReturn(Optional.of(admin));

        assertTrue(userDetailsService.loadUserByUsername("admin1") instanceof AdminPrincipal);
    }

    @Test
    void loadUserByUsername_UserOrAdminNotFound() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenThrow(new UserNotFound("not"));
        when(adminRepository.findByUsernameIgnoreCase(anyString())).thenThrow(new AdminNotFound("no admin"));
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("absent"));
    }

    @Test
    void loadAdminByUsername_NotFound_Throws() {
        when(adminRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());
        assertThrows(AdminNotFound.class, () -> userDetailsService.loadAdminByUsername("noadmin"));
    }

    @Test
    void loadUserByEmail_NotFound_Throws() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        assertThrows(UserNotFound.class, () -> userDetailsService.loadUserByEmail("noe"));
    }

    @Test
    void loadUserByEmail_ReturnsPrincipal() {
        User user = User.builder().email("mail@a.com").build();
        when(userRepository.findByEmailIgnoreCase(eq("mail@a.com"))).thenReturn(Optional.of(user));
        assertTrue(userDetailsService.loadUserByEmail("mail@a.com") instanceof UserPrincipal);
    }
}
