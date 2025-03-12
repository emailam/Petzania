package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.PetSpecies;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePetDTO {
    private String name;
    private String description;
    private String gender;
    private Integer age;
    private String breed;
    private PetSpecies species; // cat or dog,..
    private List<String> myVaccinesURLs;
    private List<String> myPicturesURLs;
}
