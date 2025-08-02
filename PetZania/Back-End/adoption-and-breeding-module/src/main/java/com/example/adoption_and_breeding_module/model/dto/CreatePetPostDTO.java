package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.validator.NotToxicText;
import com.example.adoption_and_breeding_module.validator.ValidEnum;
import jakarta.persistence.Column;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePetPostDTO {
    @NotNull(message = "Pet DTO is required.")
    @Valid
    private PetDTO petDTO;

    @Size(max = 1000, message = "Description must be at most 1000 characters.")
    @NotToxicText
    private String description;

    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0", message = "Latitude must be ≥ -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be ≤ 90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0", message = "Longitude must be ≥ -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be ≤ 180.0")
    private Double longitude;

    @NotNull(message = "Post type is required.")
    @ValidEnum(enumClass = PetPostType.class, message = "Invalid post type.")
    private PetPostType postType;
}
