package com.example.registrationmodule.model.dto;

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
    private String phoneNumber;
}
