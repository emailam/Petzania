package com.example.registrationmodule.controller;

import com.example.registrationmodule.annotation.RateLimit;
import com.example.registrationmodule.exception.admin.AdminNotFound;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.service.IAdminService;
import com.example.registrationmodule.service.IDTOConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("api")
@Tag(name = "Admin", description = "Endpoints for admin management and authentication")
@Slf4j
public class AdminController {
    private final IAdminService adminService;
    private final IDTOConversionService dtoConversionService;


    @Operation(summary = "Refresh access token using a refresh token")
    @PostMapping("/admin/refresh-token")
    public ResponseEntity<TokenDTO> refreshToken(@RequestBody RefreshTokenDTO refreshTokenDTO){
        return ResponseEntity.status(HttpStatus.OK).body(adminService.refreshToken(refreshTokenDTO.getRefreshToken()));
    }

    @Operation(summary = "Admin login and generate tokens")
    @PostMapping("/admin/login")
    public ResponseEntity<ResponseLoginDTO> login(@RequestBody @Valid LoginAdminDTO loginAdminDTO) {
        ResponseLoginDTO token = adminService.login(loginAdminDTO);
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

    @Operation(summary = "Admin logout and invalidate refresh token")
    @PostMapping("/admin/logout")
    @RateLimit
    public ResponseEntity<String> logout(@RequestBody @Valid AdminLogoutDTO adminLogoutDTO) {
        adminService.logout(adminLogoutDTO);
        return ResponseEntity.ok("Admin logged out successfully");
    }

    @Operation(summary = "Create a new admin account")
    @PostMapping(path = "/admin/create")
    @RateLimit
    public ResponseEntity<AdminDTO> createAdmin(@RequestBody @Valid AdminDTO adminDto) {
        Admin admin = dtoConversionService.mapToAdmin(adminDto);
        Admin createdAdmin = adminService.saveAdmin(admin);
        log.info("Admin: {}", admin);
        return new ResponseEntity<>(
                dtoConversionService.mapToAdminDTO(createdAdmin),
                HttpStatus.CREATED
        );
    }

    @Operation(summary = "Get an admin by ID")
    @GetMapping(path = "/admin/{id}")
    @RateLimit
    public ResponseEntity<AdminDTO> getAdminById(@PathVariable("id") UUID adminId) {
        Admin admin = adminService.getAdminById(adminId)
                .orElseThrow(() -> new AdminNotFound("Admin not found with ID: " + adminId));
        AdminDTO adminDto = dtoConversionService.mapToAdminDTO(admin);
        return new ResponseEntity<>(adminDto, HttpStatus.OK);
    }

    @Operation(summary = "Retrieve all admins")
    @GetMapping(path = "/admin/get-all")
    @RateLimit
    public ResponseEntity<List<AdminDTO>> getAllAdmins(){
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    @Operation(summary = "Delete an admin by ID")
    @DeleteMapping("/admin/delete/{id}")
    @RateLimit
    public ResponseEntity<Void> deleteAdminById(@PathVariable("id") UUID adminId) {
        if (!adminService.existsById(adminId)) {
            throw new AdminNotFound("Admin not found with ID: " + adminId);
        }
        adminService.deleteById(adminId);
        return ResponseEntity.noContent().build();
    }
}
