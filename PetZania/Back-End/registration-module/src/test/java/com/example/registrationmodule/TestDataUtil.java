package com.example.registrationmodule;

import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.enumeration.AdminRole;
import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.model.enumeration.PetSpecies;

import java.time.LocalDate;

public class TestDataUtil {
    public TestDataUtil() {
    }

    public static User createTestUser(String username) {
        return User.builder()
                .username(username)
                .password("Password123#")
                .loginTimes(0)
                .email(username + "@gmail.com")
                .name("Test User")
                .bio("I love pets!")
                .profilePictureURL("http://example.com/profile.jpg")
                .phoneNumber("1234567890")
                .verified(true)
                .online(false).isBlocked(false).build();
    }

    public static Admin createAdmin(String username) {
        return Admin.builder().
                username(username)
                .password("Password123#")
                .role(AdminRole.ADMIN)
                .build();
    }

    public static Admin createSuperAdmin(String username) {
        return Admin.builder().
                username(username)
                .password("Password123#")
                .role(AdminRole.SUPER_ADMIN)
                .build();
    }

    public static Pet createTestPet(User owner) {
        return Pet.builder()
                .name("Fluffy")
                .description("Very friendly, loves snacks")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusYears(2))
                .breed("Golden Retriever")
                .species(PetSpecies.DOG)
                .user(owner)
                .build();
    }
}
