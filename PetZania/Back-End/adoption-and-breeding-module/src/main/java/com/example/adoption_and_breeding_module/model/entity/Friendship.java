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
@Table(name = "friendships",
        indexes = {
        @Index(name = "idx_friendship_user1", columnList = "user1_id"),
        @Index(name = "idx_friendship_user2", columnList = "user2_id"),
        @Index(name = "idx_friendship_user1_user2", columnList = "user1_id, user2_id")},

        uniqueConstraints = @UniqueConstraint(
                name = "uk_friendship_user1_user2",
                columnNames = {"user1_id","user2_id"})
)
public class Friendship {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user1_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user2_id", referencedColumnName = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;
}
