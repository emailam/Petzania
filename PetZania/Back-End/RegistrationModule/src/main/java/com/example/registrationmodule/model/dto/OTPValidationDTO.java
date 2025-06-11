package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OTPValidationDTO {
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 numbers")
    private String otp;

    @Email
    private String email;
}
