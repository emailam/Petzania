package com.example.adoption_and_breeding_module.model.entity;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostStatus;
import com.example.adoption_and_breeding_module.model.enumeration.PetPostType;
import com.example.adoption_and_breeding_module.validator.NotToxicText;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "pet_posts", indexes = {
        @Index(name = "idx_post_owner", columnList = "owner_id"),
        @Index(name = "idx_post_pet", columnList = "pet_id"),
        @Index(name = "idx_post_status", columnList = "post_status"),
        @Index(name = "idx_post_type", columnList = "post_type"),
        @Index(name = "idx_post_created", columnList = "created_at")
})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PetPost {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "pet_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Pet pet;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "post_status", nullable = false)
    PetPostStatus postStatus = PetPostStatus.PENDING;

    @JoinTable(
            name = "pet_post_reactions",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
    )
    @ManyToMany(fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Builder.Default
    private Set<User> reactedUsers = new HashSet<>();

    @Builder.Default
    @Column(name = "reacts", nullable = false)
    private int reacts = 0;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "location", nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    private PetPostType postType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Transient
    private double score;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
