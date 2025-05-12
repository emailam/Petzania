package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.model.dto.MessageDTO;
import com.example.friends.and.chats.module.model.dto.SendMessageDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface IMessageService {
    MessageDTO sendMessage(SendMessageDTO sendMessageDTO, UUID senderId);

    Page<MessageDTO> getMessagesForChat(UUID chatId, UUID userId, int page, int size);

    MessageDTO getMessageById(UUID messageId, UUID userId);

    MessageDTO deleteMessage(UUID messageId, UUID userId);
}
