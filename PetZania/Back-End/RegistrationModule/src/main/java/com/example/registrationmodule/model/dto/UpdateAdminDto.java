package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAdminDto {
    private String username;
    private AdminRole adminRole;
}
