package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.AdminRole;
import com.example.registrationmodule.model.enumeration.Gender;
import com.example.registrationmodule.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAdminDto {
    @NotBlank(message = "Username is required.")
    private String username;

    @NotNull(message = "admin role is required.")
    @ValidEnum(enumClass = AdminRole.class, message = "Invalid admin role.")
    private AdminRole adminRole;
}
