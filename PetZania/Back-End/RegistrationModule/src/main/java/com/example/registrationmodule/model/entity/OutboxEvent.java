package com.example.registrationmodule.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing events that failed to be published
 * Part of the Transactional Outbox pattern
 */
@Entity
@Table(name = "outbox_events")
@Data
public class OutboxEvent {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private boolean processed;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column
    private int retryCount = 0;
}
