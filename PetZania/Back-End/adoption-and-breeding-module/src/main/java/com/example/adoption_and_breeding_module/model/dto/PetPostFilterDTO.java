package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.Gender;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.model.enumeration.PetSpecies;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetPostFilterDTO {
    private PetPostType petPostType; // ADOPTION, BREEDING
    private PetSpecies species; // "dog", "cat", or null
    private String breed;
    private Gender gender; // "male", "female", or null
    private Integer minAge;
    private Integer maxAge;
    private String sortBy = "date"; // "date" or "likes"
    private String sortOrder = "desc"; // "asc" or "desc"
}
