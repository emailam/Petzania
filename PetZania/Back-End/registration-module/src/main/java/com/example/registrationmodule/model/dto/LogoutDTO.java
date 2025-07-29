package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogoutDTO {
    @Email(message = "Invalid email format.")
    @NotBlank(message = "Email is required.")
    @Size(max = 100, message = "Email must not exceed 100 characters.")
    private String email;
    private String refreshToken;
}
