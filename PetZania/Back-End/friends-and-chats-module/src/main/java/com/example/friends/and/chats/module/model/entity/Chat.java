package com.example.friends.and.chats.module.model.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chats", indexes = {
        @Index(name = "idx_user1", columnList = "user1_id"),
        @Index(name = "idx_user2", columnList = "user2_id")
})
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user1_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user2_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime lastMessageTimestamp = LocalDateTime.MIN;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
