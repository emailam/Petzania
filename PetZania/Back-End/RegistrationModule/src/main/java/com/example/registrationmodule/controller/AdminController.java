package com.example.registrationmodule.controller;

import com.example.registrationmodule.exception.admin.AdminNotFound;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.service.IAdminService;
import com.example.registrationmodule.service.IDTOConversionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("api")
public class AdminController {
    private final IAdminService adminService;
    private final IDTOConversionService dtoConversionService;


    @PostMapping("/admin/refresh-token")
    public ResponseEntity<TokenDTO> refreshToken(@RequestBody RefreshTokenDTO refreshTokenDTO){
        return ResponseEntity.status(HttpStatus.OK).body(adminService.refreshToken(refreshTokenDTO.getRefreshToken()));
    }
    @PostMapping("/admin/login")
    public ResponseEntity<ResponseLoginDTO> login(@RequestBody @Valid LoginAdminDTO loginAdminDTO) {
        ResponseLoginDTO token = adminService.login(loginAdminDTO);
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

    @PostMapping("/admin/logout")
    public ResponseEntity<String> logout(@RequestBody @Valid AdminLogoutDTO adminLogoutDTO) {
        adminService.logout(adminLogoutDTO);
        return ResponseEntity.ok("Admin logged out successfully");
    }

    @PostMapping(path = "/admin/create")
    public ResponseEntity<AdminDTO> createAdmin(@RequestBody @Valid AdminDTO adminDto) {
        Admin admin = dtoConversionService.mapToAdmin(adminDto);
        Admin createdAdmin = adminService.saveAdmin(admin);
        System.out.println(admin);
        return new ResponseEntity<>(
                dtoConversionService.mapToAdminDTO(createdAdmin),
                HttpStatus.CREATED
        );
    }

    @GetMapping(path = "/admin/{id}")
    public ResponseEntity<AdminDTO> getAdminById(@PathVariable("id") UUID adminId) {
        Admin admin = adminService.getAdminById(adminId)
                .orElseThrow(() -> new AdminNotFound("Admin not found with ID: " + adminId));
        AdminDTO adminDto = dtoConversionService.mapToAdminDTO(admin);
        return new ResponseEntity<>(adminDto, HttpStatus.OK);
    }

    @GetMapping(path = "/admin/getAll")
    public ResponseEntity<List<AdminDTO>> getAllAdmins(){
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Void> deleteAdminById(@PathVariable("id") UUID adminId) {
        if (!adminService.existsById(adminId)) {
            throw new AdminNotFound("Admin not found with ID: " + adminId);
        }
        adminService.deleteById(adminId);
        return ResponseEntity.noContent().build();
    }
}
