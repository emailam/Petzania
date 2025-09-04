package com.example.friends.and.chats.module.model.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadCountUpdateDTO {
    private UUID userChatId;
    private int userChatUnreadCount;
    private long totalUnreadCount;
}
