package com.example.registrationmodule.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class RegisterUserDTO {
    @Size(min = 5, max = 32, message = "Username must be between 5 and 30 characters")
    private String username;

    @Email
    @NotBlank
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters long.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$&~%*()\\[\\]]).{8,}$",
            message = "Password must include at least 1 uppercase letter, 1 lowercase letter, 1 special symbol (!@#$&~%*()[]), and 1 number."
    )
    private String password;

}
