package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginAdminDTO {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
