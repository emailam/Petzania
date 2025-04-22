package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.exception.AdminNotFound;
import com.example.registrationmodule.exception.TooManyAdminRequests;
import com.example.registrationmodule.model.dto.UpdateAdminDto;
import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.repository.AdminRepository;
import com.example.registrationmodule.service.IAdminService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

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
    @RateLimiter(name = "updateAdminRateLimiter", fallbackMethod = "updateAdminFallback")
    public Admin partialUpdateAdminById(UUID adminId, UpdateAdminDto updatedAdmin) {
        return adminRepository.findById(adminId).map(existingAdmin -> {
            Optional.ofNullable(updatedAdmin.getUsername()).ifPresent(existingAdmin::setUsername);
            Optional.ofNullable(updatedAdmin.getAdminRole()).ifPresent(existingAdmin::setRole);
            return adminRepository.save(existingAdmin);
        }).orElseThrow(() -> new AdminNotFound("Admin not found with ID: " + adminId));
    }

    @Override
    @RateLimiter(name = "saveAdminRateLimiter", fallbackMethod = "saveAdminFallback")
    public Admin saveAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    @Override
    @RateLimiter(name = "deleteAdminRateLimiter", fallbackMethod = "deleteAdminFallback")
    public void deleteById(UUID adminId) {
        adminRepository.deleteById(adminId);
    }

    // Fallbacks for rate limiting
    public Admin updateAdminFallback(UUID adminId, UpdateAdminDto updatedAdmin, RequestNotPermitted ex) {
        throw new TooManyAdminRequests("Too many requests for updating admins. Please try again later.");
    }

    public Admin saveAdminFallback(Admin admin, RequestNotPermitted ex) {
        throw new TooManyAdminRequests("Too many requests for saving admins. Please try again later.");
    }

    public void deleteAdminFallback(UUID adminId, RequestNotPermitted ex) {
        throw new TooManyAdminRequests("Too many requests for deleting admins. Please try again later.");
    }
}
