package com.example.friends.and.chats.module.config;

import com.example.friends.and.chats.module.model.entity.Admin;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.enumeration.AdminRole;
import com.example.friends.and.chats.module.repository.AdminRepository;
import com.example.friends.and.chats.module.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DummyDataConfig {
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
            User user1 = User.builder()
                    .username("johndoe")
                    .email("john@example.com")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .build();

            User user2 = User.builder()
                    .username("janedoe")
                    .email("jane@example.com")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .build();

            User user3 = User.builder()
                    .username("petmaster")
                    .email("petmaster@example.com")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .build();

            User user4 = User.builder()
                    .username("vetfriend")
                    .email("vet@example.com")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .build();

            User user5 = User.builder()
                    .username("shopkeeper123")
                    .email("shop@example.com")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .build();

            List<User> users = new ArrayList<>(List.of(user1, user2, user3, user4, user5));
            for (User user : users) {
                if (userRepository.findByUsernameIgnoreCase(user.getUsername()).isEmpty()) {
                    userRepository.save(user);
                }
            }
        };
    }
}
