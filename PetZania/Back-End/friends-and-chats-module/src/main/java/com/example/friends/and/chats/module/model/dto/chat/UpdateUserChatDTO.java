package com.example.friends.and.chats.module.model.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserChatDTO {
    private boolean pinned;
    private boolean unread;
    private boolean muted;
}
