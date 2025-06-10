package com.example.friends.and.chats.module.model.dto.friend;

import com.example.friends.and.chats.module.model.dto.user.UserDTO;
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
public class FriendRequestDTO {
    private UUID requestId;
    private UserDTO sender;
    private UserDTO receiver;
    private Timestamp createdAt;
}
