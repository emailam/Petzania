package com.example.registrationmodule.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_username", columnList = "username"),
                @Index(name = "idx_email", columnList = "email"),
                @Index(name = "idx_verified", columnList = "verified"),
                @Index(name = "idx_is_blocked", columnList = "is_blocked"),
        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "username", length = 32, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Column(name = "login_times")
    private Integer loginTimes = 0;
    @Column(name = "email", length = 100, nullable = false, unique = true)
    @Email
    private String email;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "bio", length = 255)
    private String bio;

    @Column(name = "profile_picture_url", length = 255)
    private String profilePictureURL;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "verification_code", length = 10)
    private String verificationCode;

    @Column(name = "reset_code", length = 10)
    private String resetCode;

    @Column(name = "reset_code_expiration_time")
    private Timestamp resetCodeExpirationTime;

    @Column(name = "expiration_time")
    private Timestamp expirationTime;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "online", nullable = false)
    private boolean online = false;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Pet> myPets;
}
