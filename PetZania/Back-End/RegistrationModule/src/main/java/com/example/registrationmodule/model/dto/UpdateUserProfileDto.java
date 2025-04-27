package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserProfileDto {
    private String name;
    private String bio;
    private String profilePictureURL;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format.")
    private String phoneNumber;
}
