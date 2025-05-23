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
public class FriendshipDTO {
    private UUID friendshipId;
    private UserDTO user1;
    private UserDTO user2;
    private Timestamp createdAt;
}
