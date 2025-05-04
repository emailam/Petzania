package com.example.registrationmodule.repository;

import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
    Optional<Admin> findByUsernameIgnoreCase(String username);
}
