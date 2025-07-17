package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.PetPostStatus;
import com.example.adoption_and_breeding_module.validator.NotToxicText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePetPostDTO {
    @Valid
    private UpdatePetDTO updatePetDTO;

    @NotToxicText
    private String description;

    private Double latitude;
    private Double longitude;
    PetPostStatus postStatus;
}
