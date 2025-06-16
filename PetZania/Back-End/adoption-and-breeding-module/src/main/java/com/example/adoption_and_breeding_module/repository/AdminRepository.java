package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
    Optional<Admin> findByUsernameIgnoreCase(String username);
}
