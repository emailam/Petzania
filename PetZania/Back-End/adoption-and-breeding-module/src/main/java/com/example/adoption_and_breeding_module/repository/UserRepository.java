package com.example.adoption_and_breeding_module.repository;

import com.example.adoption_and_breeding_module.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
