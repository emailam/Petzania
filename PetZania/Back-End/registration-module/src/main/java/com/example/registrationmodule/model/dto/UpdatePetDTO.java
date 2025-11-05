package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.model.enumeration.PetSpecies;
import com.example.registrationmodule.validator.ValidEnum;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePetDTO {
    @Size(max = 50, message = "Name must not exceed 50 characters.")
    private String name;
    @Size(max = 255, message = "Description must not exceed 255 characters.")
    private String description;

    @ValidEnum(enumClass = Gender.class, message = "Invalid gender value.")
    private Gender gender;

    private LocalDate dateOfBirth;

    @Size(max = 32, message = "Breed must not exceed 32 characters.")
    private String breed;

    // species: validated by @ValidEnum, DB column is VARCHAR(32)
    @ValidEnum(enumClass = PetSpecies.class, message = "Invalid species value.")
    private PetSpecies species; // cat or dog,..

    private List<String> myVaccinesURLs;
    private List<String> myPicturesURLs;
}
