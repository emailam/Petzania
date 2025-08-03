package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserProfileDto {
    @Size(max = 50, message = "Name must not exceed 50 characters.")
    private String name;
    @Size(max = 255, message = "Bio must not exceed 255 characters.")
    private String bio;
    @Size(max = 255, message = "Profile picture url must not exceed 255 characters.")
    private String profilePictureURL;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format.")
    @Size(max = 20, message = "Phone number must not exceed 20 characters.")
    private String phoneNumber;
}
