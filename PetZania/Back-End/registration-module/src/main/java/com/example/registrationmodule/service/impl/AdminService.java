package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.admin.AdminNotFound;
import com.example.registrationmodule.exception.authenticationAndVerificattion.RefreshTokenNotValid;
import com.example.registrationmodule.exception.user.UserAlreadyLoggedOut;
import com.example.registrationmodule.exception.user.UsernameAlreadyExists;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.enumeration.AdminRole;
import com.example.registrationmodule.repository.AdminRepository;
import com.example.registrationmodule.service.IAdminService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class AdminService implements IAdminService {
    private final AdminRepository adminRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final DTOConversionService dtoConversionService;

    @Override
    public boolean existsById(UUID adminId) {
        return adminRepository.existsById(adminId);
    }

    @Override
    public ResponseLoginDTO login(LoginAdminDTO loginAdminDTO) {
        Admin admin = adminRepository.findByUsernameIgnoreCase(loginAdminDTO.getUsername()).orElseThrow(() -> new AdminNotFound("Username or password is incorrect"));

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginAdminDTO.getUsername(), loginAdminDTO.getPassword()));
        if (authentication.isAuthenticated()) {
            String role;
            // if this is the credentials of the super admin.
            if (admin.getRole() == AdminRole.SUPER_ADMIN) {
                role = "ROLE_SUPER_ADMIN";
            } else {
                role = "ROLE_ADMIN";
            }
            String message = "Successful login";
            TokenDTO tokenDTO = new TokenDTO(jwtService.generateAccessToken(loginAdminDTO.getUsername(), role),
                    jwtService.generateRefreshToken(loginAdminDTO.getUsername(), role));
            int loginTimes = 0; // ignore this for the admin.
            UUID adminId = admin.getAdminId();
            return new ResponseLoginDTO(message, tokenDTO, 0, adminId);
        } else {
            throw new AdminNotFound("Email or password is incorrect");
        }
    }

    @Override
    public TokenDTO refreshToken(String refreshToken) {
        if (refreshToken == null) {
            throw new RefreshTokenNotValid("There is no refresh token sent");
        }

        if (refreshTokenService.isTokenRevoked(refreshToken)) {
            throw new RefreshTokenNotValid("The refresh token is invalid");
        }

        String username = jwtService.extractEmail(refreshToken);
        String role = jwtService.extractRole(refreshToken);
        boolean isExpired = jwtService.isTokenExpired(refreshToken);

        if (username == null || role == null || isExpired) {
            throw new RefreshTokenNotValid("Invalid or expired refresh token");
        }

        if (adminRepository.findByUsernameIgnoreCase(username).isEmpty()) {
            throw new AdminNotFound("Admin does not exist");
        }

        String newAccessToken = jwtService.generateAccessToken(username, role);
        return new TokenDTO(newAccessToken, refreshToken);
    }

    @Override
    public List<AdminDTO> getAllAdmins() {
        List<Admin> admins = adminRepository.findAll();
        return admins.stream()
                .map(dtoConversionService::mapToAdminDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void logout(AdminLogoutDTO adminLogoutDTO) {
        Admin admin = adminRepository.findByUsernameIgnoreCase(adminLogoutDTO.getUsername()).orElseThrow(() -> new AdminNotFound("Admin does not exist"));

        if (refreshTokenService.isTokenRevoked(adminLogoutDTO.getRefreshToken())) {
            throw new UserAlreadyLoggedOut("Admin already logged out");
        }

        // save it in the database
        refreshTokenService.saveToken(adminLogoutDTO.getRefreshToken());
    }

    @Override
    public Optional<Admin> getAdminById(UUID adminId) {
        return adminRepository.findById(adminId);
    }

    @Override
    public Admin saveAdmin(Admin admin) {
        if (adminRepository.findByUsernameIgnoreCase(admin.getUsername()).isPresent()) {
            throw new UsernameAlreadyExists("Username already exists");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }

    @Override
    public void deleteById(UUID adminId) {
        if (!adminRepository.existsById(adminId)) {
            throw new AdminNotFound("Admin not found with ID: " + adminId);
        }
        adminRepository.deleteById(adminId);
    }
}
