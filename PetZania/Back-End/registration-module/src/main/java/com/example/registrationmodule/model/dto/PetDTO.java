package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.model.enumeration.PetSpecies;
import com.example.registrationmodule.validator.ValidEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PetDTO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // Clients can't send petId
    private UUID petId;

    @NotBlank(message = "Name is required.")
    @Size(max = 50, message = "Name must not exceed 50 characters.")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters.")
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
    @Size(max = 32, message = "Breed must not exceed 32 characters.")
    private String breed;

    @NotNull(message = "Species is required.")
    @ValidEnum(enumClass = PetSpecies.class, message = "Invalid species value.")
    private PetSpecies species;

    @Size(max = 255, message = "Each vaccine URL must not exceed 255 characters.")
    private List<@Size(max = 255, message = "Each vaccine URL must not exceed 255 characters.") String> myVaccinesURLs;
    @Size(max = 255, message = "Each picture URL must not exceed 255 characters.")
    private List<@Size(max = 255, message = "Each picture URL must not exceed 255 characters.") String> myPicturesURLs;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID userId;
}
