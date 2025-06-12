package com.example.friends.and.chats.module.repository;

import com.example.friends.and.chats.module.model.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
    Optional<Admin> findByUsernameIgnoreCase(String username);
}
