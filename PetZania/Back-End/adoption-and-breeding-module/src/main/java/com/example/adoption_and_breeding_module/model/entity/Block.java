package com.example.adoption_and_breeding_module.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Table(name = "blocks",
        indexes = {
        // Composite index for blocker + blocked
        @Index(name = "idx_block_blocker_blocked", columnList = "blocker_id, blocked_id"),
        // Index for finding blocked users
        @Index(name = "idx_blocked_users", columnList = "blocker_id")},

        uniqueConstraints = @UniqueConstraint(
                name = "uk_block_blocker_blocked",
                columnNames = {"blocker_id","blocked_id"})
)
public class Block {
    @Id
    @Column(name = "block_id", nullable = false, updatable = false)
    private UUID blockId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "blocker_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User blocker;

    @ManyToOne(optional = false)
    @JoinColumn(name = "blocked_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;
}
