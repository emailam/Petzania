package com.example.registrationmodule.repository;

import com.example.registrationmodule.model.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminRepository  extends JpaRepository<Admin, UUID> {
}
