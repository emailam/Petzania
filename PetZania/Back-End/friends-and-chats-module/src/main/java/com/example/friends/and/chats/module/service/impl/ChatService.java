package com.example.friendsAndChatsModule.service.impl;

import com.example.friendsAndChatsModule.exception.user.UserNotFound;
import com.example.friendsAndChatsModule.model.dto.ChatDTO;
import com.example.friendsAndChatsModule.model.entity.Chat;
import com.example.friendsAndChatsModule.model.entity.User;
import com.example.friendsAndChatsModule.repository.ChatRepository;
import com.example.friendsAndChatsModule.repository.UserRepository;
import com.example.friendsAndChatsModule.service.IChatService;
import com.example.friendsAndChatsModule.service.IDTOConversionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class ChatService implements IChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final IDTOConversionService dtoConversionService;

    @Override
    public ChatDTO createChatIfNotExists(UUID user1Id, UUID user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new UserNotFound("User 1 not found"));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new UserNotFound("User 2 not found"));

        Optional<Chat> existingChat = chatRepository.findByUsers(user1, user2);

        if (existingChat.isPresent()) {
            return dtoConversionService.mapToChatDTO(existingChat.get());
        }

        Chat newChat =
                Chat.builder()
                .user1(user1)
                .user2(user2)
                .build();

        Chat savedChat = chatRepository.save(newChat);

        return dtoConversionService.mapToChatDTO(savedChat);
    }
}
