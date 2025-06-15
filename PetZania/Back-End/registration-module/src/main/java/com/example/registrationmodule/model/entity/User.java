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

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "login_times")
    private Integer loginTimes = 0;
    @Column(name = "email", nullable = false, unique = true)
    @Email
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "bio")
    private String bio;

    @Column(name = "profile_picture_url")
    private String profilePictureURL;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "verification_code")
    private String verificationCode;

    private String resetCode;
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
