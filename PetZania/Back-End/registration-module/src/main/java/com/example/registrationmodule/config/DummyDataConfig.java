package com.example.registrationmodule.config;

import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.enumeration.AdminRole;
import com.example.registrationmodule.repository.AdminRepository;
import com.example.registrationmodule.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
public class DummyDataConfig {
    @Bean
    CommandLineRunner seedDatabase(AdminRepository adminRepository, UserRepository userRepository) {
        return args -> {
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

            // Super Admin
            Admin superAdmin = Admin.builder()
                    .username("superadmin")
                    .password(passwordEncoder.encode("superadmin"))
                    .role(AdminRole.SUPER_ADMIN)
                    .build();
            if (adminRepository.findByUsernameIgnoreCase(superAdmin.getUsername()).isEmpty()) {
                adminRepository.save(superAdmin);
            }

            // Users
            User user1 = User.builder()
                    .username("user1")
                    .password(passwordEncoder.encode("password"))
                    .email("user1@gmail.com")
                    .name("John Doe")
                    .bio("Just a test user.")
                    .verified(true)
                    .isBlocked(false)
                    .loginTimes(10)
                    .phoneNumber("01129588407")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .online(false)
                    .build();

            User user2 = User.builder()
                    .username("user2")
                    .password(passwordEncoder.encode("password"))
                    .email("user2@gmail.com")
                    .name("Jane Doe")
                    .bio("Pet lover and volunteer.")
                    .verified(true)
                    .isBlocked(false)
                    .loginTimes(7)
                    .phoneNumber("01124985463")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .online(false)
                    .build();

            User user3 = User.builder()
                    .username("user3")
                    .password(passwordEncoder.encode("password"))
                    .email("user3@gmail.com")
                    .name("Pet Master")
                    .bio("Managing multiple pet profiles.")
                    .verified(false)
                    .isBlocked(true)
                    .loginTimes(3)
                    .phoneNumber("01112233455")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .online(false)
                    .build();

            User user4 = User.builder()
                    .username("user4")
                    .password(passwordEncoder.encode("password"))
                    .email("user4@gmail.com")
                    .name("Dr. Vet")
                    .bio("Experienced veterinarian.")
                    .verified(true)
                    .isBlocked(false)
                    .loginTimes(5)
                    .phoneNumber("01111654322")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .online(false)
                    .build();

            User user5 = User.builder()
                    .username("user5")
                    .password(passwordEncoder.encode("password"))
                    .email("user5@gmail.com")
                    .name("Shop Keeper")
                    .bio("Owner of the local pet shop.")
                    .verified(true)
                    .isBlocked(false)
                    .loginTimes(123)
                    .phoneNumber("01111654329")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .online(false)
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
