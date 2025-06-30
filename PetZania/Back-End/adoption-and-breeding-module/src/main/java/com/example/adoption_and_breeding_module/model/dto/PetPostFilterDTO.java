package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.*;
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
    private PetPostType petPostType = PetPostType.ALL; // ADOPTION, BREEDING, ALL

    @Builder.Default
    private PetSpecies species = PetSpecies.ALL; // "dog", "cat", etc, or ALL

    @Builder.Default
    private PetPostStatus petPostStatus = PetPostStatus.PENDING;

    @Builder.Default
    private String breed = "ALL"; // ALL

    @Builder.Default
    private Gender gender = Gender.ALL; // "male", "female", or ALL

    @Builder.Default
    private Integer minAge = null;

    @Builder.Default
    private Integer maxAge = null;

    @Builder.Default
    private PetPostSortBy sortBy = PetPostSortBy.SCORE; // "created_date", "reacts", or "score"

    @Builder.Default
    private boolean sortDesc = true;
}
