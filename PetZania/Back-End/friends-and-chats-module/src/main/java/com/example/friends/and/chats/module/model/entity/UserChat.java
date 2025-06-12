package com.example.friends.and.chats.module.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_chats", indexes = {
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_chat", columnList = "chat_id"),
        @Index(name = "idx_pinned", columnList = "pinned"),
        @Index(name = "idx_unread", columnList = "unread"),
        @Index(name = "idx_muted", columnList = "muted")
})
public class UserChat {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_chat_id", nullable = false)
    private UUID userChatId;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "unread", nullable = false)
    private boolean unread = false;

    @Column(name = "muted", nullable = false)
    private boolean muted = false;
}
