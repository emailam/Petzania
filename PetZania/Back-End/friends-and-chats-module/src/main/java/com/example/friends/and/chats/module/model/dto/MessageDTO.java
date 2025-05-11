package com.example.friends.and.chats.module.model.dto;

import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
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
public class MessageDTO {
    private UUID messageId;
    private UUID chatId;
    private UUID senderId;
    private String content;
    private UUID replyToMessageId;
    private LocalDateTime sentAt;
    private MessageStatus status;
    private boolean isFile;
    private boolean isEdited;
}