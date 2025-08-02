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
@Table(name = "follows",
        indexes = {
        // Index for finding all followers of a user
        @Index(name = "idx_follow_followed", columnList = "followed_id"),
        // Index for finding all users followed by a user
        @Index(name = "idx_follow_follower", columnList = "follower_id")},

        uniqueConstraints = @UniqueConstraint(
                name = "uk_follow_follower_followed",
                columnNames = {"follower_id","followed_id"})
)
public class Follow {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "follower_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User follower;

    @ManyToOne(optional = false)
    @JoinColumn(name = "followed_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User followed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

}
