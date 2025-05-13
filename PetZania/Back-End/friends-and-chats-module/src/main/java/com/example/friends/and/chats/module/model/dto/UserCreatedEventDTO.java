package com.example.friends.and.chats.module.model.dto;

import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCreatedEventDTO {
    private UUID eventId;
    private String moduleName;
    @Id
    @NotBlank
    private UUID userId;

    @Size(min = 5, max = 32, message = "Username must be between 5 and 30 characters")
    private String username;

    @Email
    @NotBlank
    private String email;

    private String profilePictureURL;

}
