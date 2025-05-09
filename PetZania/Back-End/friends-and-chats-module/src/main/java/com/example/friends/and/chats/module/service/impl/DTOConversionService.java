package com.example.friendsAndChatsModule.service.impl;

import com.example.friendsAndChatsModule.exception.user.UserNotFound;
import com.example.friendsAndChatsModule.model.dto.ChatDTO;
import com.example.friendsAndChatsModule.model.entity.Chat;
import com.example.friendsAndChatsModule.model.entity.User;
import com.example.friendsAndChatsModule.repository.UserRepository;
import com.example.friendsAndChatsModule.service.IDTOConversionService;
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
}
