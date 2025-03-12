package com.example.registrationmodule.controller;

import com.example.registrationmodule.model.dto.*;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.service.IAdminService;
import com.example.registrationmodule.service.IDTOConversionService;
import com.example.registrationmodule.service.impl.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
        Optional<Admin> admin = adminService.getAdminById(adminId);
        return admin.map(adminEntity -> {
            AdminDto adminDto = dtoConversionService.mapToAdminDto(adminEntity);
            return new ResponseEntity<>(adminDto, HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping(path = "/admin/{id}")
    public ResponseEntity<AdminDto> partialUpdateUAdminById(@PathVariable("id") UUID adminId,
                                                            @RequestBody UpdateAdminDto updateAdminDto) {

        if (!adminService.existsById(adminId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Admin updatedAdmin = adminService.partialUpdateAdminById(adminId, updateAdminDto);
        return new ResponseEntity<>(
                dtoConversionService.mapToAdminDto(updatedAdmin),
                HttpStatus.OK
        );
    }
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<AdminService> deleteAdminById(@PathVariable("id") UUID adminId) {
        if (!adminService.existsById(adminId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        adminService.deleteById(adminId);
        return ResponseEntity.noContent().build();
    }
}
