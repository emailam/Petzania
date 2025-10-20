package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.*;
import com.example.adoption_and_breeding_module.validator.ValidEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetPostFilterDTO {
    @Builder.Default
    @ValidEnum(enumClass = PetPostType.class, message = "Invalid post type.")
    private PetPostType petPostType = PetPostType.ALL; // ADOPTION, BREEDING, ALL

    @Builder.Default
    @ValidEnum(enumClass = PetSpecies.class, message = "Invalid species.")
    private PetSpecies species = PetSpecies.ALL; // "dog", "cat", etc, or ALL

    @Builder.Default
    @ValidEnum(enumClass = PetPostStatus.class, message = "Invalid post status.")
    private PetPostStatus petPostStatus = PetPostStatus.PENDING;

    @Builder.Default
    @Size(max = 32, message = "Breed must be at most 32 characters.")
    private String breed = "ALL"; // ALL

    @Builder.Default
    @ValidEnum(enumClass = Gender.class, message = "Invalid gender.")
    private Gender gender = Gender.ALL; // "male", "female", or ALL

    @Builder.Default
    @Min(value = 0, message = "Min age must be at least 0.")
    private Integer minAge = null;

    @Builder.Default
    @Min(value = 0, message = "Max age must be at least 0.")
    private Integer maxAge = null;

    @Builder.Default
    @ValidEnum(enumClass = PetPostSortBy.class, message = "Invalid post sort by.")
    private PetPostSortBy sortBy = PetPostSortBy.SCORE; // "created_date", "reacts", or "score"

    @Builder.Default
    private boolean sortDesc = true;
}
