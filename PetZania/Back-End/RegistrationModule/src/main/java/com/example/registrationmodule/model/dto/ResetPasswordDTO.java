package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 numbers")
    private String otp;

    @Email
    private String email;


    @Size(min = 8, message = "Password must be at least 8 characters long.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$&~%*()\\[\\]]).{8,}$",
            message = "Password must include at least 1 uppercase letter, 1 lowercase letter, 1 special symbol (!@#$&~%*()[]), and 1 number."
    )
    private String password;

}
