package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.Gender;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import com.example.adoption_and_breeding_module.validator.ValidEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PetDTO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // Clients can't send petId
    private UUID petId;

    @NotBlank(message = "Name is required.")
    private String name;

    private String description;

    @NotNull(message = "Gender is required.")
    @ValidEnum(enumClass = Gender.class, message = "Invalid gender value.")
    private Gender gender;

    @NotNull(message = "Date of birth is required.")
    private LocalDate dateOfBirth;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // Exclude from requests
    @NotNull(message = "Age is required.")
    private String age;

    @NotBlank(message = "Breed is required.")
    private String breed;

    @NotNull(message = "Species is required.")
    @ValidEnum(enumClass = PetSpecies.class, message = "Invalid species value.")
    private PetSpecies species;

    private List<String> myVaccinesURLs;
    private List<String> myPicturesURLs;
}
