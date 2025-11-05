package com.example.adoption_and_breeding_module;

import com.example.adoption_and_breeding_module.model.dto.PetDTO;
import com.example.adoption_and_breeding_module.model.entity.Pet;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.enumeration.Gender;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
public class TestDataUtil {
    public static User createTestUser(String username) {
        return User.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .email(username + "@gmail.com")
                .latitude(35.8617)
                .longitude(104.1954)
                .build();
    }

    public static PetDTO createPetDTO(String name, PetSpecies species, Gender gender) {
        PetDTO dto = new PetDTO();
        dto.setName(name);
        dto.setSpecies(species);
        dto.setGender(gender);
        dto.setDateOfBirth(LocalDate.now().minusYears(1));
        dto.setBreed("Test Breed");
        dto.setDescription("Test description");
        dto.setMyVaccinesURLs(List.of("https://example.com/vaccine.jpg"));
        dto.setMyPicturesURLs(List.of("https://example.com/picture.jpg"));
        return dto;
    }

    public static Pet createTestPet(String name, PetSpecies species, Gender gender, int ageInMonths) {
        LocalDate dateOfBirth = LocalDate.now().minusMonths(ageInMonths);
        return Pet.builder()
                .name(name)
                .species(species)
                .gender(gender)
                .dateOfBirth(dateOfBirth)
                .breed("Test Breed")
                .description("Test pet description")
                .myVaccinesURLs(List.of("https://example.com/vaccine.jpg"))
                .myPicturesURLs(List.of("https://example.com/picture.jpg"))
                .build();
    }
}
