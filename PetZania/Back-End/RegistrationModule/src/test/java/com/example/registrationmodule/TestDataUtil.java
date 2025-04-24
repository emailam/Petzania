package com.example.registrationmodule;

import com.example.registrationmodule.model.entity.Pet;
import com.example.registrationmodule.model.entity.User;
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

    public static Pet createTestPetA(User owner) {
        return Pet.builder()
                .name("Fluffy")
                .description("Very friendly, loves snacks")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusYears(2))
                .breed("Golden Retriever")
                .species(PetSpecies.DOG)
                .myVaccinesURLs(List.of(
                        "https://example.org/vaccines/rabies.jpg",
                        "https://example.org/vaccines/distemper.jpg"))
                .myPicturesURLs(List.of(
                        "https://example.org/pics/fluffy1.jpg",
                        "https://example.org/pics/fluffy2.jpg"))
                .user(owner)
                .build();
    }
}
