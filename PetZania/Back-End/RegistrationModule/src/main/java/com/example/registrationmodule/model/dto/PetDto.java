package com.example.registrationmodule.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PetDto {
    private UUID petId;
    private String name;
    private String description;
    private String gender;
    private int age;
    private String breed;
    private String species; // cat or dog,..
    private List<String> myVaccinesURLs;
    private List<String> myPicturesURLs;
}
