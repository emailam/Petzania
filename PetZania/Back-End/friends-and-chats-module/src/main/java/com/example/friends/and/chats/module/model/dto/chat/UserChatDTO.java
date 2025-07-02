package com.example.friends.and.chats.module.model.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChatDTO {
    private UUID userChatId;
    private UUID chatId;
    private UUID userId;
    private Boolean pinned;
    private Integer unread;
    private Boolean muted;
}
