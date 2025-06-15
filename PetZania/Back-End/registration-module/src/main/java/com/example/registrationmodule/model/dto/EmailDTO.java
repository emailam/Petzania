package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class EmailDTO {
    @Email
    private String email;
}
