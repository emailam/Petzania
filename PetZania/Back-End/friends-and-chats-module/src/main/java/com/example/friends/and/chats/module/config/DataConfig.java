package com.example.friends.and.chats.module.config;

import com.example.friends.and.chats.module.model.entity.Admin;
import com.example.friends.and.chats.module.model.enumeration.AdminRole;
import com.example.friends.and.chats.module.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class DataConfig {

    @Value("${spring.super.admin.username}")
    private String superAdminUsername;

    @Bean
    CommandLineRunner seedDatabase(AdminRepository adminRepository) {
        return args -> {
            // Super Admin
            Admin superAdmin = Admin.builder()
                    .username(superAdminUsername)
                    .role(AdminRole.SUPER_ADMIN)
                    .build();
            if (adminRepository.findByUsernameIgnoreCase(superAdmin.getUsername()).isEmpty()) {
                adminRepository.save(superAdmin);
            }
        };
    }
}
