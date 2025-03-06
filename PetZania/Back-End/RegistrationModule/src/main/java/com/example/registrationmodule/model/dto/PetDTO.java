package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.PetSpecies;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PetDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // This prevents clients from sending petId
    private UUID petId;
    private String name;
    private String description;
    private String gender;
    private Integer age;
    private String breed;
    private PetSpecies species; // cat or dog,..
    private List<String> myVaccinesURLs;
    private List<String> myPicturesURLs;
    private UUID userId;
}
