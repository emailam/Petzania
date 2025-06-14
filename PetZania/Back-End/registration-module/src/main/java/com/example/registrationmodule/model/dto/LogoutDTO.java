package com.example.registrationmodule.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogoutDTO {
    private String email;
    private String refreshToken;
}
