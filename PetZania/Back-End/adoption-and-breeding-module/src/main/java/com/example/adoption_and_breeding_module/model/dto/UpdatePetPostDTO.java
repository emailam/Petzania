package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.PetPostStatus;
import com.example.adoption_and_breeding_module.validator.NotToxicText;
import com.example.adoption_and_breeding_module.validator.ValidEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 1000, message = "Description must be at most 1000 characters.")
    private String description;

    @DecimalMin(value = "-90.0", message = "Latitude must be ≥ -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be ≤ 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be ≥ -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be ≤ 180.0")
    private Double longitude;

    @ValidEnum(enumClass = PetPostStatus.class, message = "Invalid post status.")
    PetPostStatus postStatus;
}
