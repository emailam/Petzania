package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.AdminRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDTO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // This prevents clients from sending adminId
    private UUID adminId;

    @Size(min = 5, max = 32, message = "Username must be between 5 and 30 characters")
    private String username;

    @Size(min = 8, message = "Password must be at least 8 characters long.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$&~%*()\\[\\]]).{8,}$",
            message = "Password must include at least 1 uppercase letter, 1 lowercase letter, 1 special symbol (!@#$&~%*()[]), and 1 number."
    )
    private String password;

    @NotNull(message = "admin role is required.")
    private AdminRole adminRole;
}
