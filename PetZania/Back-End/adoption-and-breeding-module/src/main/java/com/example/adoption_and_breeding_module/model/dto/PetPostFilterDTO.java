package com.example.adoption_and_breeding_module.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetPostFilterDTO {
    private String category; // "dog", "cat", or null
    private String breed;
    private String gender; // "male", "female", or null
    private Integer minAge;
    private Integer maxAge;
    private String sortBy = "date"; // "date" or "likes"
    private String sortOrder = "desc"; // "asc" or "desc"
}
