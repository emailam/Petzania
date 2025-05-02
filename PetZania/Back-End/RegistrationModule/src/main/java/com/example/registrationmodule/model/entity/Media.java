package com.example.registrationmodule.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID mediaId;

    private String key; // S3 key

    private String type; // Content type like image/jpeg, video/mp4

    private String format; // jpg, png, mp4, etc.

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
