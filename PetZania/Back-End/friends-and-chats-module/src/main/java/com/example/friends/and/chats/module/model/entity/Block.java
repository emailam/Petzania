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
@Table(name = "blocks", indexes = {
        // Composite index for blocker + blocked
        @Index(name = "idx_block_blocker_blocked", columnList = "blocker_id, blocked_id"),
        // Index for finding blocked users
        @Index(name = "idx_blocked_users", columnList = "blocker_id")})
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "blocker_id", referencedColumnName = "user_id")
    private User blocker;

    @ManyToOne
    @JoinColumn(name = "blocked_id", referencedColumnName = "user_id")
    private User blocked;

    private Timestamp createdAt;
}
