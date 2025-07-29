package com.example.registrationmodule.config;

import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.enumeration.AdminRole;
import com.example.registrationmodule.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class DataConfig {

    @Value("${spring.super.admin.username}")
    private String superAdminUsername;
    @Value("${spring.super.admin.password}")
    private String superAdminPassword;
    @Bean
    CommandLineRunner seedDatabase(AdminRepository adminRepository) {
        return args -> {
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

            // Super Admin
            Admin superAdmin = Admin.builder()
                    .username(superAdminUsername)
                    .password(passwordEncoder.encode(superAdminPassword))
                    .role(AdminRole.SUPER_ADMIN)
                    .build();
            if (adminRepository.findByUsernameIgnoreCase(superAdmin.getUsername()).isEmpty()) {
                adminRepository.save(superAdmin);
            }
        };
    }
}
