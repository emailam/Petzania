package com.example.friends.and.chats.module.model.event;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserEvent {
    @org.hibernate.validator.constraints.UUID
    UUID userId;

    @Size(min = 5, max = 32, message = "Username must be between 5 and 30 characters")
    private String username;
    @Email
    @NotBlank
    private String email;
}