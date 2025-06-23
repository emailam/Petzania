package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.PetPostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePetPostDTO {
    private UpdatePetDTO updatePetDTO;
    private String description;
    PetPostStatus postStatus;
}
