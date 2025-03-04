package com.example.registrationmodule.model.entity;

import com.example.registrationmodule.model.enumeration.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userId;


    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    private String password;

    @Email
    private String email;
    private String name;
    private String bio;
    private String profilePictureURL;
    private String phoneNumber;
    private String verificationCode;
    private Timestamp expirationTime;
    private boolean verified;
    private List<UserRole> userRoles;

    // private List<UUID> savedPosts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Pet> myPets;
    // private List<User> friends;
    // private List<User> followers;
    // private List<User> following;
    // private List<User> blocked;
    // private List<User> receivedFriendRequests;
    // private List<UUID> sendFriendRequests;
    // private List<UUID> notifications;
    // private List<UUID> myChats;
    // private List<UUID> archivedChats;
    // private List<UUID> pinnedGroups;
    // private UUID storeProfileId;
    // private UUID vetProfileId;
    // Payment Method

}
