package com.example.registrationmodule.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(max = 32, message = "Username must not exceed 32 characters.")
    private String username;

    @Email(message = "Invalid email format.")
    @NotBlank(message = "Email is required.")
    @Size(max = 100, message = "Email must not exceed 100 characters.")
    private String email;

    @NotBlank(message = "Name is required.")
    @Size(max = 50, message = "Name must not exceed 50 characters.")
    private String name;
    private Integer loginTimes;

    @Size(max = 255, message = "Bio must not exceed 255 characters.")
    private String bio;

    @Size(max = 255, message = "Profile picture url must not exceed 255 characters.")
    private String profilePictureURL;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format.")
    @Size(max = 20, message = "Phone number must not exceed 20 characters.")
    private String phoneNumber;


    private List<PetDTO> myPets;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean blocked;

    private boolean online;

}
