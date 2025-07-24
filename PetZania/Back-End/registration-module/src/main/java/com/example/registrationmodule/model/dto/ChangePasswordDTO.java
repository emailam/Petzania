package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {
    @Email(message = "Invalid email format.")
    @NotBlank(message = "Email is required.")
    @Size(max = 100, message = "Email must not exceed 100 characters.")
    private String email;

    @Size(min = 8, max = 32, message = "Password must be between 8 and 32 characters.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$&~%*()\\[\\]]).{8,}$",
            message = "Password must include at least 1 uppercase letter, 1 lowercase letter, 1 special symbol (!@#$&~%*()[]), and 1 number."
    )
    private String newPassword;
}
