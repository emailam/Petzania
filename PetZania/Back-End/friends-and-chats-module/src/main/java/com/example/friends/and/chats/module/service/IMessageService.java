package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.model.dto.message.MessageDTO;
import com.example.friends.and.chats.module.model.dto.message.MessageReactionDTO;
import com.example.friends.and.chats.module.model.dto.message.SendMessageDTO;
import com.example.friends.and.chats.module.model.entity.MessageReaction;
import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface IMessageService {
    MessageDTO sendMessage(SendMessageDTO sendMessageDTO, UUID senderId);

    Page<MessageDTO> getMessagesByChat(UUID chatId, UUID userId, int page, int size);

    MessageDTO getMessageById(UUID messageId, UUID userId);

    void deleteMessage(UUID messageId, UUID userId);

    MessageDTO updateMessageContent(UUID messageId, UUID userId, String content);

    MessageDTO updateMessageStatus(UUID messageId, UUID userId, MessageStatus messageStatus);

    MessageReactionDTO reactToMessage(UUID messageId, UUID userId, MessageReact reactionType);

    UUID getChatIdFromMessageId(UUID messageId);

    void removeReaction(UUID messageId, UUID userId);

    List<MessageReactionDTO> getReactionsForMessage(UUID messageId, UUID userId);
    long getTotalUnreadCount(UUID userId);
}
