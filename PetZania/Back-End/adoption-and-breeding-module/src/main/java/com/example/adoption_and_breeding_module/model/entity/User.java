package com.example.adoption_and_breeding_module.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "users", indexes = @Index(name="idx_username", columnList="username"))
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @EqualsAndHashCode.Include
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "latitude", nullable = false)
    @Builder.Default
    private Double latitude = 0.0;

    @Column(name = "longitude", nullable = false)
    @Builder.Default
    private Double longitude = 0.0;
}

