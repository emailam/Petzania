package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.exception.chat.ChatNotFound;
import com.example.friends.and.chats.module.exception.chat.UserChatNotFound;
import com.example.friends.and.chats.module.exception.user.UserAccessDenied;
import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.dto.chat.ChatDTO;
import com.example.friends.and.chats.module.model.dto.chat.UpdateUserChatDTO;
import com.example.friends.and.chats.module.model.dto.chat.UserChatDTO;
import com.example.friends.and.chats.module.model.entity.Chat;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.entity.UserChat;
import com.example.friends.and.chats.module.repository.UserChatRepository;
import com.example.friends.and.chats.module.repository.UserRepository;
import com.example.friends.and.chats.module.service.IChatService;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.friends.and.chats.module.repository.ChatRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class ChatService implements IChatService {

    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
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

        UserChat userChat1 =
                UserChat.builder()
                        .chat(savedChat)
                        .user(user1)
                        .build();
        UserChat userChat2 =
                UserChat.builder()
                        .chat(savedChat)
                        .user(user1)
                        .build();

        userChatRepository.save(userChat1);
        userChatRepository.save(userChat2);

        return dtoConversionService.mapToChatDTO(savedChat);
    }

    @Override
    public List<ChatDTO> getChatsForUser(UUID userId) {

        if(!userRepository.existsById(userId)) {
            throw new UserNotFound("User not found with id: " + userId);
        }

        List<UserChat> userChats = userChatRepository.findByUser_UserId(userId);

        return userChats.stream()
                .map(userChat -> dtoConversionService.mapToChatDTO(userChat.getChat()))
                .collect(Collectors.toList());
    }

    @Override
    public UserChatDTO partialUpdateUserChat(UUID chatId, UUID userId, UpdateUserChatDTO updateUserChatDTO) {
        return userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId).map(existingUserChat -> {
            Optional.of(updateUserChatDTO.isPinned()).ifPresent(existingUserChat::setPinned);
            Optional.of(updateUserChatDTO.isUnread()).ifPresent(existingUserChat::setUnread);
            Optional.of(updateUserChatDTO.isMuted()).ifPresent(existingUserChat::setMuted);

            UserChat updatedUserChat = userChatRepository.save(existingUserChat);
            return dtoConversionService.mapToUserChatDTO(updatedUserChat);
        }).orElseThrow(() -> new UserChatNotFound("User chat does not exist"));
    }

    @Override
    public ChatDTO getChatById(UUID chatId, UUID userId) {

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFound("Chat does not exist"));
        if(chat.getUser1().getUserId() != userId && chat.getUser2().getUserId() != userId) {
            throw new UserAccessDenied("You can only get your chats");
        }

        return dtoConversionService.mapToChatDTO(chat);
    }

    @Override
    public void deleteUserChatById(UUID userChatId, UUID userId) {
        UserChat chat = userChatRepository.findById(userChatId)
                .orElseThrow(() -> new ChatNotFound("User chat does not exist"));
        if(chat.getUser().getUserId() != userId) {
            throw new UserAccessDenied("You can only delete your chats");
        }

        userChatRepository.deleteById(userChatId);
    }
}
