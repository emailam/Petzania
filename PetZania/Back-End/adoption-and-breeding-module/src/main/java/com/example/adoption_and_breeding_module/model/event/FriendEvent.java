package com.example.adoption_and_breeding_module.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FriendEvent {
    @org.hibernate.validator.constraints.UUID
    private UUID friendshipId;

    @org.hibernate.validator.constraints.UUID
    private UUID user1Id;

    @org.hibernate.validator.constraints.UUID
    private UUID user2Id;

    private Timestamp createdAt;
} 