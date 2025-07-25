package com.example.adoption_and_breeding_module.model.entity;

import com.example.adoption_and_breeding_module.model.enumeration.InterestType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "pet_post_interests", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
public class PetPostInterest {
    @EmbeddedId
    private PetPostInterestId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("postId")
    @JoinColumn(name = "post_id", nullable = false)
    private PetPost post;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false)
    private InterestType interestType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }
} 