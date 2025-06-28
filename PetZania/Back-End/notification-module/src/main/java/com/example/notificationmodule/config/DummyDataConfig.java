package com.example.notificationmodule.config;

import com.example.notificationmodule.model.entity.Admin;
import com.example.notificationmodule.model.entity.User;
import com.example.notificationmodule.model.enumeration.AdminRole;
import com.example.notificationmodule.repository.AdminRepository;
import com.example.notificationmodule.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
public class DummyDataConfig {
    public User createUser(String username) {
        return User.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .email(username + "@gmail.com")
                .build();
    }

    @Bean
    CommandLineRunner seedDatabase(AdminRepository adminRepository, UserRepository userRepository) {
        return args -> {
            // Super Admin
            Admin superAdmin = Admin.builder()
                    .username("superadmin")
                    .role(AdminRole.SUPER_ADMIN)
                    .build();
            if (adminRepository.findByUsernameIgnoreCase(superAdmin.getUsername()).isEmpty()) {
                adminRepository.save(superAdmin);
            }

            // Users
            User user1 = createUser("user1");
            User user2 = createUser("user2");
            User user3 = createUser("user3");
            User user4 = createUser("user4");
            User user5 = createUser("user5");

            List<User> users = new ArrayList<>(List.of(user1, user2, user3, user4, user5));
            for (User user : users) {
                if (userRepository.findByUsernameIgnoreCase(user.getUsername()).isEmpty()) {
                    userRepository.save(user);
                }
            }
        };
    }
}
