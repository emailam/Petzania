package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.PetPostStatus;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.validator.NotToxicText;
import com.example.adoption_and_breeding_module.validator.ValidEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetPostDTO {

    @NotNull(message = "Post ID is required.")
    private UUID postId;

    @NotNull(message = "Owner ID is required.")
    private UUID ownerId;

    @NotNull(message = "Pet details are required.")
    @Valid
    private PetDTO petDTO;

    @NotNull(message = "Post status is required.")
    @ValidEnum(enumClass = PetPostStatus.class, message = "Invalid post status.")
    private PetPostStatus postStatus;

    @NotNull(message = "Reacted users set must not be null.")
    private Set<@NotNull(message = "User ID cannot be null") UUID> reactedUsersIds;

    @Min(value = 0, message = "Reacts count cannot be negative.")
    private int reacts;

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

    @NotNull(message = "Creation timestamp is required.")
    @PastOrPresent(message = "Creation time cannot be in the future.")
    private Instant createdAt;

    @NotNull(message = "Update timestamp is required.")
    @PastOrPresent(message = "Update time cannot be in the future.")
    private Instant updatedAt;
}

