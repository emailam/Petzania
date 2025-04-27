package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // Prevents clients from sending userId
    private UUID userId;

    @NotBlank(message = "Username is required.")
    private String username;

    @NotBlank(message = "Name is required.")
    private String name;

    private String bio;

    private String profilePictureURL;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format.")
    private String phoneNumber;

    private List<UserRole> userRoles;

    private List<PetDTO> myPets;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // These counts should be calculated server-side
    private int friendsCount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int followersCount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int followingCount;

    private UUID storeProfileId;

    private UUID vetProfileId;
}
