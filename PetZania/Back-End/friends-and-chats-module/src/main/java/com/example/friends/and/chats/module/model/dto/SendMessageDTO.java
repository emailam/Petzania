package com.example.friends.and.chats.module.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatDTO {

    private UUID chatId;
    private UUID user1Id;
    private UUID user2Id;
    private LocalDateTime createdAt;

}
