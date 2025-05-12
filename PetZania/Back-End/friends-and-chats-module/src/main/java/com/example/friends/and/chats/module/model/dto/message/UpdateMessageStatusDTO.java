package com.example.friends.and.chats.module.model.dto.message;

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
public class UpdateMessageStatusDTO {
    private MessageStatus messageStatus;
}