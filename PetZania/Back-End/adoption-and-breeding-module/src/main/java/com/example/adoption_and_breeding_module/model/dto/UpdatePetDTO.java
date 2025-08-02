package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.Gender;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import com.example.adoption_and_breeding_module.validator.NotToxicText;
import com.example.adoption_and_breeding_module.validator.ValidEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class UpdatePetDTO {

    @NotToxicText
    @Size(max = 50, message = "Name must be at most 50 characters.")
    private String name;

    @NotToxicText
    @Size(max = 255, message = "Description must be at most 255 characters.")
    private String description;

    @ValidEnum(enumClass = Gender.class, message = "Invalid gender value.")
    private Gender gender;

    @NotToxicText
    @Size(max = 32, message = "Breed must be at most 32 characters.")
    private String breed;

    @ValidEnum(enumClass = PetSpecies.class, message = "Invalid species value.")
    private PetSpecies species;

    @Size(max = 50, message = "At most 50 vaccine URLs.")
    private List<@NotBlank @Pattern(
            regexp = "https?://.*",
            message = "Each vaccine URL must be a valid HTTP/HTTPS URL"
    ) String> myVaccinesURLs;

    @Size(max = 150, message = "At most 150 picture URLs.")
    private List<@NotBlank @Pattern(
            regexp = "https?://.*",
            message = "Each picture URL must be a valid HTTP/HTTPS URL"
    ) String> myPicturesURLs;
}
