package com.example.friends.and.chats.module.model.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageDTO {

    @NotNull
    private UUID chatId;

    @Size(max = 1000)
    private String content;

    private UUID replyToMessageId;

    private boolean isFile;
}
