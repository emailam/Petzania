package com.example.registrationmodule.model.entity;

import com.example.registrationmodule.model.enumeration.UserRole;
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

    @Column(name = "email", nullable = false, unique = true)
    @Email
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "bio")
    private String bio;

    @Column(name = "profile_picture_url")
    private String profilePictureURL;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "expiration_time")
    private Timestamp expirationTime;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked;

    @ElementCollection(targetClass = UserRole.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private List<UserRole> userRoles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Pet> myPets;

    @ElementCollection
    @CollectionTable(
            name = "user_friends",
            joinColumns = @JoinColumn(name = "user_id"),
            indexes = @Index(name = "idx_friends_user", columnList = "user_id")
    )
    @Column(name = "friend_id")
    private List<UUID> friends;

    @ElementCollection
    @CollectionTable(
            name = "user_followers",
            joinColumns = @JoinColumn(name = "user_id"),
            indexes = @Index(name = "idx_followers_user", columnList = "user_id")
    )
    @Column(name = "follower_id")
    private List<UUID> followers;

    @ElementCollection
    @CollectionTable(
            name = "user_following",
            joinColumns = @JoinColumn(name = "user_id"),
            indexes = @Index(name = "idx_followings_user", columnList = "user_id")
    )
    @Column(name = "following_id")
    private List<UUID> following;

    @ElementCollection
    @CollectionTable(
            name = "user_blocked",
            joinColumns = @JoinColumn(name = "user_id"),
            indexes = @Index(name = "idx_blocked_user", columnList = "user_id")
    )
    @Column(name = "blocked_id")
    private List<UUID> blocked;

    // private List<User> receivedFriendRequests;
    // private List<UUID> sendFriendRequests;
    // private List<UUID> notifications;
    // private List<UUID> myChats;
    // private List<UUID> archivedChats;
    // private List<UUID> pinnedGroups;

    @Column(name = "store_profile_id")
    private UUID storeProfileId;

    @Column(name = "vet_profile_id")
    private UUID vetProfileId;
    // Payment Method
}
