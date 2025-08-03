package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OTPValidationDTO {
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 numbers.")
    private String otp;

    @Email(message = "Invalid email format.")
    @NotBlank(message = "Email is required.")
    @Size(max = 100, message = "Email must not exceed 100 characters.")
    private String email;
}
