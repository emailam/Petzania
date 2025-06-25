package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.validator.NotToxicText;
import com.example.adoption_and_breeding_module.validator.ValidEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotToxicText
    private String description;

    @NotBlank(message = "Pet DTO is required.")
    @NotToxicText
    private String location;

    @NotNull(message = "Post type is required.")
    @ValidEnum(enumClass = PetPostType.class, message = "Invalid post type.")
    private PetPostType postType;
}
