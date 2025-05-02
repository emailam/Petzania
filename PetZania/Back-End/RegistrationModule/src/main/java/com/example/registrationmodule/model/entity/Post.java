package com.example.registrationmodule.model.entity;

import com.example.registrationmodule.model.dto.MediaDTO;
import com.example.registrationmodule.model.enumeration.Mood;
import com.example.registrationmodule.model.enumeration.Visibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID postId;

    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mood mood;

    private boolean pinned;
    private boolean turnedOnNotifications;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
