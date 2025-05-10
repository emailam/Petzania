package com.example.friendsAndChatsModule.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Table(name = "friendships", indexes = {
        // Index for user1 + user2
        @Index(name = "idx_friendship_user1_user2", columnList = "user1_id, user2_id"),
        // Reverse index for user2 + user1
        @Index(name = "idx_friendship_user2_user1", columnList = "user2_id, user1_id")
})
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user1_id", referencedColumnName = "user_id")
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user2_id", referencedColumnName = "user_id")
    private User user2;

    private Timestamp createdAt;
}
