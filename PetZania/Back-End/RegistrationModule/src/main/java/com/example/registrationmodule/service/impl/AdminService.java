package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.model.dto.UpdateAdminDto;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.repository.AdminRepository;
import com.example.registrationmodule.service.IAdminService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class AdminService implements IAdminService {
    private final AdminRepository adminRepository;

    @Override
    public boolean existsById(UUID adminId) {
        return adminRepository.existsById(adminId);
    }

    @Override
    public Optional<Admin> getAdminById(UUID adminId) {
        return adminRepository.findById(adminId);
    }

    @Override
    public Admin partialUpdateAdminById(UUID adminId, UpdateAdminDto updatedAdmin) {
        return adminRepository.findById(adminId).map(existingAdmin -> {
            Optional.ofNullable(updatedAdmin.getUsername()).ifPresent(existingAdmin::setUsername);
            Optional.ofNullable(updatedAdmin.getAdminRole()).ifPresent(existingAdmin::setRole);
            return adminRepository.save(existingAdmin);
        }).orElseThrow(() -> new RuntimeException("Admin does not exist"));
    }

    @Override
    public Admin saveAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    @Override
    public void deleteById(UUID adminId) {
        adminRepository.deleteById(adminId);
    }
}
