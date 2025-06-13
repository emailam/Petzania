package com.example.adoption_and_breeding_module.model.entity;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostStatus;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "pet_posts", indexes = {
        @Index(name = "idx_post_owner", columnList = "owner_id"),
        @Index(name = "idx_post_pet", columnList = "pet_id"),
        @Index(name = "idx_post_status", columnList = "post_status"),
        @Index(name = "idx_post_type", columnList = "post_type"),
        @Index(name = "idx_post_created", columnList = "created_at")
})
public class PetPost {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_status", nullable = false)
    PetPostStatus postStatus;

    private int reactions;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    private PetPostType postType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
        postStatus = PetPostStatus.PENDING;
        reactions = 0;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
