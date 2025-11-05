package com.example.friends.and.chats.module.model.event;

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
public class FollowEvent {
    @org.hibernate.validator.constraints.UUID
    private UUID followId;

    @org.hibernate.validator.constraints.UUID
    private UUID followerId;

    @org.hibernate.validator.constraints.UUID
    private UUID followedId;

    private Timestamp createdAt;
} 