package com.example.registrationmodule.service;

import com.example.registrationmodule.exception.admin.AdminNotFound;
import com.example.registrationmodule.exception.authenticationAndVerificattion.RefreshTokenNotValid;
import com.example.registrationmodule.exception.user.UserAlreadyLoggedOut;
import com.example.registrationmodule.exception.user.UsernameAlreadyExists;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.enumeration.AdminRole;
import com.example.registrationmodule.repository.AdminRepository;
import com.example.registrationmodule.service.impl.AdminService;
import com.example.registrationmodule.service.impl.DTOConversionService;
import com.example.registrationmodule.service.impl.JWTService;
import com.example.registrationmodule.service.impl.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {
    @Mock private AdminRepository adminRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JWTService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private DTOConversionService dtoConversionService;
    @InjectMocks private AdminService adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void existsById_true() {
        UUID id = UUID.randomUUID();
        when(adminRepository.existsById(id)).thenReturn(true);
        assertTrue(adminService.existsById(id));
    }

    @Test
    void existsById_false() {
        UUID id = UUID.randomUUID();
        when(adminRepository.existsById(id)).thenReturn(false);
        assertFalse(adminService.existsById(id));
    }

    @Test
    void login_success() {
        LoginAdminDTO dto = new LoginAdminDTO();
        dto.setUsername("admin");
        dto.setPassword("pass");
        Admin admin = Admin.builder().adminId(UUID.randomUUID()).username("admin").password("pass").role(AdminRole.ADMIN).build();
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh");
        ResponseLoginDTO result = adminService.login(dto);
        assertEquals("Successful login", result.getMessage());
        assertEquals("access", result.getTokenDTO().getAccessToken());
        assertEquals("refresh", result.getTokenDTO().getRefreshToken());
        assertEquals(admin.getAdminId(), result.getUserId());
    }

    @Test
    void login_wrongUsername_throws() {
        LoginAdminDTO dto = new LoginAdminDTO();
        dto.setUsername("notfound");
        when(adminRepository.findByUsernameIgnoreCase("notfound")).thenReturn(Optional.empty());
        assertThrows(AdminNotFound.class, () -> adminService.login(dto));
    }

    @Test
    void login_wrongPassword_throws() {
        LoginAdminDTO dto = new LoginAdminDTO();
        dto.setUsername("admin");
        dto.setPassword("wrong");
        Admin admin = Admin.builder().adminId(UUID.randomUUID()).username("admin").password("pass").role(AdminRole.ADMIN).build();
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(false);
        assertThrows(AdminNotFound.class, () -> adminService.login(dto));
    }

    @Test
    void refreshToken_success() {
        String refreshToken = "refresh";
        Admin admin = Admin.builder().adminId(UUID.randomUUID()).username("admin").password("pass").role(AdminRole.ADMIN).build();
        when(refreshTokenService.isTokenRevoked(refreshToken)).thenReturn(false);
        when(jwtService.extractEmail(refreshToken)).thenReturn("admin");
        when(jwtService.extractRole(refreshToken)).thenReturn("ROLE_ADMIN");
        when(jwtService.isTokenExpired(refreshToken)).thenReturn(false);
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("newAccess");
        TokenDTO result = adminService.refreshToken(refreshToken);
        assertEquals("newAccess", result.getAccessToken());
        assertEquals(refreshToken, result.getRefreshToken());
    }

    @Test
    void refreshToken_null_throws() {
        assertThrows(RefreshTokenNotValid.class, () -> adminService.refreshToken(null));
    }

    @Test
    void refreshToken_revoked_throws() {
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(true);
        assertThrows(RefreshTokenNotValid.class, () -> adminService.refreshToken("refresh"));
    }

    @Test
    void refreshToken_expired_throws() {
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(false);
        when(jwtService.extractEmail("refresh")).thenReturn("admin");
        when(jwtService.extractRole("refresh")).thenReturn("ROLE_ADMIN");
        when(jwtService.isTokenExpired("refresh")).thenReturn(true);
        assertThrows(RefreshTokenNotValid.class, () -> adminService.refreshToken("refresh"));
    }

    @Test
    void refreshToken_notFound_throws() {
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(false);
        when(jwtService.extractEmail("refresh")).thenReturn("admin");
        when(jwtService.extractRole("refresh")).thenReturn("ROLE_ADMIN");
        when(jwtService.isTokenExpired("refresh")).thenReturn(false);
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());
        assertThrows(AdminNotFound.class, () -> adminService.refreshToken("refresh"));
    }

    @Test
    void getAllAdmins_returnsList() {
        Admin admin = Admin.builder().adminId(UUID.randomUUID()).username("admin").password("pass").role(AdminRole.ADMIN).build();
        List<Admin> admins = List.of(admin);
        when(adminRepository.findAll()).thenReturn(admins);
        AdminDTO dto = new AdminDTO();
        when(dtoConversionService.mapToAdminDTO(admin)).thenReturn(dto);
        List<AdminDTO> result = adminService.getAllAdmins();
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    void logout_success() {
        AdminLogoutDTO dto = new AdminLogoutDTO("admin", "refresh");
        Admin admin = Admin.builder().adminId(UUID.randomUUID()).username("admin").password("pass").role(AdminRole.ADMIN).build();
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(false);
        when(refreshTokenService.saveToken("refresh")).thenReturn(true);
        assertDoesNotThrow(() -> adminService.logout(dto));
    }

    @Test
    void logout_alreadyLoggedOut_throws() {
        AdminLogoutDTO dto = new AdminLogoutDTO("admin", "refresh");
        Admin admin = Admin.builder().adminId(UUID.randomUUID()).username("admin").password("pass").role(AdminRole.ADMIN).build();
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(refreshTokenService.isTokenRevoked("refresh")).thenReturn(true);
        assertThrows(UserAlreadyLoggedOut.class, () -> adminService.logout(dto));
    }

    @Test
    void logout_notFound_throws() {
        AdminLogoutDTO dto = new AdminLogoutDTO("notfound", "refresh");
        when(adminRepository.findByUsernameIgnoreCase("notfound")).thenReturn(Optional.empty());
        assertThrows(AdminNotFound.class, () -> adminService.logout(dto));
    }

    @Test
    void getAdminById_found() {
        UUID id = UUID.randomUUID();
        Admin admin = Admin.builder().adminId(id).username("admin").password("pass").role(AdminRole.ADMIN).build();
        when(adminRepository.findById(id)).thenReturn(Optional.of(admin));
        assertEquals(Optional.of(admin), adminService.getAdminById(id));
    }

    @Test
    void getAdminById_notFound() {
        UUID id = UUID.randomUUID();
        when(adminRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), adminService.getAdminById(id));
    }

    @Test
    void saveAdmin_success() {
        Admin admin = Admin.builder().adminId(UUID.randomUUID()).username("admin").password("pass").role(AdminRole.ADMIN).build();
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());
        when(adminRepository.save(any())).thenReturn(admin);
        Admin result = adminService.saveAdmin(admin);
        assertEquals(admin, result);
    }

    @Test
    void saveAdmin_usernameExists_throws() {
        Admin admin = Admin.builder().adminId(UUID.randomUUID()).username("admin").password("pass").role(AdminRole.ADMIN).build();
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        assertThrows(UsernameAlreadyExists.class, () -> adminService.saveAdmin(admin));
    }

    @Test
    void deleteById_success() {
        UUID id = UUID.randomUUID();
        when(adminRepository.existsById(id)).thenReturn(true);
        doNothing().when(adminRepository).deleteById(id);
        assertDoesNotThrow(() -> adminService.deleteById(id));
    }

    @Test
    void deleteById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(adminRepository.existsById(id)).thenReturn(false);
        assertThrows(AdminNotFound.class, () -> adminService.deleteById(id));
    }
} 