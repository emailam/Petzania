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
                    .username("johndoe")
                    .password(passwordEncoder.encode("password"))
                    .email("john@example.com")
                    .name("John Doe")
                    .bio("Just a test user.")
                    .verified(true)
                    .isBlocked(false)
                    .loginTimes(10)
                    .phoneNumber("01129588407")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .build();

            User user2 = User.builder()
                    .username("janedoe")
                    .password(passwordEncoder.encode("password"))
                    .email("jane@example.com")
                    .name("Jane Doe")
                    .bio("Pet lover and volunteer.")
                    .verified(true)
                    .isBlocked(false)
                    .loginTimes(7)
                    .phoneNumber("01124985463")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .build();

            User user3 = User.builder()
                    .username("petmaster")
                    .password(passwordEncoder.encode("password"))
                    .email("petmaster@example.com")
                    .name("Pet Master")
                    .bio("Managing multiple pet profiles.")
                    .verified(false)
                    .isBlocked(true)
                    .loginTimes(3)
                    .phoneNumber("01112233455")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .build();

            User user4 = User.builder()
                    .username("vetfriend")
                    .password(passwordEncoder.encode("password"))
                    .email("vet@example.com")
                    .name("Dr. Vet")
                    .bio("Experienced veterinarian.")
                    .verified(true)
                    .isBlocked(false)
                    .loginTimes(5)
                    .phoneNumber("01111654322")
                    .profilePictureURL("https://images.unsplash.com/photo-1633332755192-727a05c4013d?fm=jpg&q=60&w=3000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8dXNlcnxlbnwwfHwwfHx8MA%3D%3D")
                    .build();

            User user5 = User.builder()
                    .username("shopkeeper123")
                    .password(passwordEncoder.encode("password"))
                    .email("shop@example.com")
                    .name("Shop Keeper")
                    .bio("Owner of the local pet shop.")
                    .verified(true)
                    .isBlocked(false)
                    .loginTimes(123)
                    .phoneNumber("01111654329")
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
