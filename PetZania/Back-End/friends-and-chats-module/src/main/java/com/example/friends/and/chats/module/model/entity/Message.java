package com.example.friends.and.chats.module.model.entity;

import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_chat", columnList = "chat_id"),
        @Index(name = "idx_message_sender", columnList = "sender_id")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User sender;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "reply_to_id")
    private Message replyTo;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "is_file", nullable = false)
    private boolean isFile;

    @Column(name = "is_edited", nullable = false)
    private boolean isEdited = false;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MessageReaction> reactions = new ArrayList<>();

    @PrePersist
    public void onSend() {
        this.sentAt = LocalDateTime.now();
    }
}

