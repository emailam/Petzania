package com.example.registrationmodule.controller;

import com.example.registrationmodule.exception.AdminNotFound;
import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.service.IAdminService;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.service.impl.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("api")
public class AdminController {
    private final IAdminService adminService;
    private final IDTOConversionService dtoConversionService;

    @PostMapping(path = "/admin")
    public ResponseEntity<AdminDto> createAdminById(@RequestBody UpdateAdminDto adminDto) {
        Admin admin = dtoConversionService.mapToAdmin(adminDto);
        Admin createdAdmin = adminService.saveAdmin(admin);
        return new ResponseEntity<>(
                dtoConversionService.mapToAdminDto(createdAdmin),
                HttpStatus.CREATED
        );
    }

    @GetMapping(path = "/admin/{id}")
    public ResponseEntity<AdminDto> getAdminById(@PathVariable("id") UUID adminId) {
        Admin admin = adminService.getAdminById(adminId)
                .orElseThrow(() -> new AdminNotFound("Admin not found with ID: " + adminId));
        AdminDto adminDto = dtoConversionService.mapToAdminDto(admin);
        return new ResponseEntity<>(adminDto, HttpStatus.OK);
    }

    @PatchMapping(path = "/admin/{id}")
    public ResponseEntity<AdminDto> partialUpdateUAdminById(@PathVariable("id") UUID adminId,
                                                            @RequestBody UpdateAdminDto updateAdminDto) {

        if (!adminService.existsById(adminId)) {
            throw new AdminNotFound("Admin not found with ID: " + adminId);
        }

        Admin updatedAdmin = adminService.partialUpdateAdminById(adminId, updateAdminDto);
        return new ResponseEntity<>(
                dtoConversionService.mapToAdminDto(updatedAdmin),
                HttpStatus.OK
        );
    }
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteAdminById(@PathVariable("id") UUID adminId) {
        if (!adminService.existsById(adminId)) {
            throw new AdminNotFound("Admin not found with ID: " + adminId);
        }
        adminService.deleteById(adminId);
        return ResponseEntity.noContent().build();
    }
}
