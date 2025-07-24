package com.example.notificationmodule.model.entity;


import com.example.notificationmodule.model.enumeration.NotificationStatus;
import com.example.notificationmodule.model.enumeration.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notifications", indexes = {
        @Index(name = "idx_recipient", columnList = "recipient_id")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "message", nullable = false, length = 255)
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    // Store dynamic attributes as JSON
    @ElementCollection
    @CollectionTable(name = "notification_attributes",
            joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value", length = 1000)
    private Map<String, String> attributes;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "initiator_id", nullable = false)
    private UUID initiatorId;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}