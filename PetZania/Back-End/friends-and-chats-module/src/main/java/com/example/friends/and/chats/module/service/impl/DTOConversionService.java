package com.example.friends.and.chats.module.service.impl;


import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.dto.chat.ChatDTO;
import com.example.friends.and.chats.module.model.dto.message.MessageDTO;
import com.example.friends.and.chats.module.model.dto.chat.UserChatDTO;
import com.example.friends.and.chats.module.model.dto.message.MessageReactionDTO;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.repository.UserRepository;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
public class DTOConversionService implements IDTOConversionService {

    private final UserRepository userRepository;

    @Override
    public ChatDTO mapToChatDTO(Chat chat) {
        if (chat == null) {
            return null;
        }

        return ChatDTO.builder()
                .chatId(chat.getChatId())
                .user1Id(chat.getUser1().getUserId())
                .user2Id(chat.getUser2().getUserId())
                .createdAt(chat.getCreatedAt())
                .build();
    }

    @Override
    public Chat mapToChat(ChatDTO chatDTO) {
        if (chatDTO == null) {
            return null;
        }

        User user1 = userRepository.findById(chatDTO.getUser1Id())
                .orElseThrow(() -> new UserNotFound("User1 not found"));
        User user2 = userRepository.findById(chatDTO.getUser2Id())
                .orElseThrow(() -> new UserNotFound("User2 not found"));

        return Chat.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(chatDTO.getCreatedAt())
                .build();
    }

    @Override
    public UserChatDTO mapToUserChatDTO(UserChat userChat) {
        if (userChat == null)
            return null;

        return UserChatDTO.builder()
                .userChatId(userChat.getUserChatId())
                .chatId(userChat.getChat().getChatId())
                .userId(userChat.getUser().getUserId())
                .pinned(userChat.isPinned())
                .unread(userChat.isUnread())
                .muted(userChat.isMuted())
                .build();
    }

    @Override
    public MessageDTO mapToMessageDTO(Message message) {
        if (message == null) return null;

        return MessageDTO.builder()
                .messageId(message.getMessageId())
                .chatId(message.getChat().getChatId())
                .senderId(message.getSender().getUserId())
                .content(message.getContent())
                .replyToMessageId(
                        message.getReplyTo() != null ? message.getReplyTo().getMessageId() : null
                )
                .sentAt(message.getSentAt())
                .status(message.getStatus())
                .isFile(message.isFile())
                .isEdited(message.isEdited())
                .build();
    }

    @Override
    public MessageReactionDTO mapToMessageReactionDTO(MessageReaction messageReaction) {
        if(messageReaction == null) return null;

        return MessageReactionDTO.builder()
                .messageReactionId(messageReaction.getMessageReactionId())
                .messageId(messageReaction.getMessage().getMessageId())
                .userId(messageReaction.getUser().getUserId())
                .reactionType(messageReaction.getReactionType())
                .build();
    }

}
