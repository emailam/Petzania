package com.example.registrationmodule.model.dto;

import com.example.registrationmodule.model.enumeration.AdminRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // This prevents clients from sending petId
    private UUID adminId;
    private String username;
    private AdminRole adminRole;
}
