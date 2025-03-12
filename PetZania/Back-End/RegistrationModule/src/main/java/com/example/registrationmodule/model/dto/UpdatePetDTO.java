package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.model.enumeration.PetSpecies;
import com.example.registrationmodule.validator.ValidEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePetDTO {
    private String name;
    private String description;

    @ValidEnum(enumClass = Gender.class, message = "Invalid gender value.")
    private Gender gender;

    private LocalDate dateOfBirth;

    private String breed;

    @ValidEnum(enumClass = PetSpecies.class, message = "Invalid species value.")
    private PetSpecies species; // cat or dog,..

    private List<String> myVaccinesURLs;
    private List<String> myPicturesURLs;
}
