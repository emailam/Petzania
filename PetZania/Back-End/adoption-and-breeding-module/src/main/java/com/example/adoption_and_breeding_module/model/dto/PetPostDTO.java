package com.example.adoption_and_breeding_module.model.dto;

import com.example.adoption_and_breeding_module.model.enumeration.PetPostStatus;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetPostDTO {
    private UUID postId;
    private UUID ownerId;
    private PetDTO petDTO;
    PetPostStatus postStatus;
    private Set<UUID> reactedUsersIds;
    private int reacts;
    private String description;
    private String location;
    private PetPostType postType;
    private Instant createdAt;
    private Instant updatedAt;
}
