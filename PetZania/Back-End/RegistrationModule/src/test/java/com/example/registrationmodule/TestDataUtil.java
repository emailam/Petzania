package com.example.registrationmodule;

import com.example.registrationmodule.model.entity.Admin;
import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.enumeration.AdminRole;
import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.model.enumeration.PetSpecies;
import com.example.registrationmodule.model.enumeration.UserRole;
import com.example.registrationmodule.service.IUserService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestDataUtil {
    public TestDataUtil() {
    }

    public static User createTestUserA() {
        return User.builder()
                .username("testUser")
                .password("Password123#")
                .loginTimes(0)
                .email("test@example.com")
                .name("Test User")
                .bio("I love pets!")
                .profilePictureURL("http://example.com/profile.jpg")
                .phoneNumber("1234567890")
                .userRoles(List.of(UserRole.VET))
                .friends(new ArrayList<>())
                .followers(new ArrayList<>())
                .following(new ArrayList<>())
                .storeProfileId(UUID.randomUUID())
                .vetProfileId(UUID.randomUUID())
                .verified(true)
                .build();
    }
    public static User createTestUserB() {
        return User.builder()
                .username("testUserB")
                .password("Password123#")
                .loginTimes(0)
                .email("testB@example.com")
                .name("Test User")
                .bio("I love pets!")
                .profilePictureURL("http://example.com/profile.jpg")
                .phoneNumber("1234567890")
                .userRoles(List.of(UserRole.VET))
                .friends(new ArrayList<>())
                .followers(new ArrayList<>())
                .following(new ArrayList<>())
                .storeProfileId(UUID.randomUUID())
                .vetProfileId(UUID.randomUUID())
                .verified(true)
                .build();
    }

    public static Admin createAdminA() {
        return Admin.builder().
                username("admin")
                .password("Password123#")
                .role(AdminRole.ADMIN)
                .build();
    }

    public static Admin createAdminB() {
        return Admin.builder()
                .username("adminB")
                .password("Password123#")
                .role(AdminRole.ADMIN)
                .build();
    }

    public static Admin createAdminC() {
        return Admin.builder()
                .username("adminC")
                .password("Password123#")
                .role(AdminRole.ADMIN)
                .build();
    }

    public static Admin createSuperAdminA() {
        return Admin.builder().
                username("superadminA")
                .password("Password123#")
                .role(AdminRole.SUPER_ADMIN)
                .build();
    }

    public static Pet createTestPetA(User owner) {
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
