package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // This prevents clients from sending petId
    private UUID userId;
    private String username;
    private String name;
    private String bio;
    private String profilePictureURL;
    private String phoneNumber;
    private List<UserRole> userRoles;
    private List<PetDto> myPets;
    private int friendsCount;
    private int followersCount;
    private int followingCount;
    private UUID storeProfileId;
    private UUID vetProfileId;
}
