package com.example.friends.and.chats.module.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowDTO {
    private UUID followId;
    private UserDTO follower;
    private UserDTO followed;
    private Timestamp createdAt;
}
