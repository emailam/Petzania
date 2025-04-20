package com.example.registrationmodule.model.entity;


import com.example.registrationmodule.model.enumeration.AdminRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(
        name = "admins",
        indexes = {
                @Index(name = "idx_admin_username", columnList = "username"),
                @Index(name = "idx_admin_role", columnList = "role")
        }
)
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private AdminRole role;
}
