package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminLogoutDTO
{
    @Size(min = 5, max = 32, message = "Username must be between 5 and 32 characters.")
    private String username;
    private String refreshToken;
}
