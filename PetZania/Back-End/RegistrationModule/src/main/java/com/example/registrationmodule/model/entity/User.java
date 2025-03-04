package com.example.registrationmodule.model.entity;

import com.example.registrationmodule.model.enumeration.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

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
    private String username;
    private String password;
    private String email;
    private String name;
    private String bio;
    private String profilePictureURL;
    private String phoneNumber;

    @ElementCollection(targetClass = UserRole.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private List<UserRole> userRoles;

    // private List<UUID> posts;
    // private List<UUID> savedPosts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Pet> myPets;

    @ElementCollection
    private List<UUID> friends;

    @ElementCollection
    private List<UUID> followers;

    @ElementCollection
    private List<UUID> following;

    @ElementCollection
    private List<UUID> blocked;
    // private List<User> receivedFriendRequests;
    // private List<UUID> sendFriendRequests;
    // private List<UUID> notifications;
    // private List<UUID> myChats;
    // private List<UUID> archivedChats;
    // private List<UUID> pinnedGroups;
    private UUID storeProfileId;
    private UUID vetProfileId;
    // Payment Method
}
