package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginAdminDTO {
    @Size(min = 5, max = 32, message = "Username must be between 5 and 32 characters.")
    private String username;
    @NotBlank
    private String password;
}
