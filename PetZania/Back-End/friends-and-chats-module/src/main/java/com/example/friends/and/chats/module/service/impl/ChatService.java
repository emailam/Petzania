package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.exception.RateLimitExceeded;
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
import com.example.friends.and.chats.module.repository.ChatRepository;
import com.example.friends.and.chats.module.repository.UserChatRepository;
import com.example.friends.and.chats.module.repository.UserRepository;
import com.example.friends.and.chats.module.service.IChatService;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class ChatService implements IChatService {

    private static final String CHAT_RATE_LIMITER = "chatServiceRateLimiter";

    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
    private final UserRepository userRepository;
    private final IDTOConversionService dtoConversionService;

    @Override
    @RateLimiter(name = CHAT_RATE_LIMITER, fallbackMethod = "rateLimitFallbackCreate")
    public ChatDTO createChatIfNotExists(UUID user1Id, UUID user2Id) {
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Cannot create a chat with yourself");
        }

        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new UserNotFound("User 1 not found"));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new UserNotFound("User 2 not found"));

        Chat chat = chatRepository.findByUsers(user1, user2)
                .orElseGet(() -> chatRepository.save(
                        Chat.builder()
                                .user1(user1)
                                .user2(user2)
                                .build()
                ));

        for (User user : List.of(user1, user2)) {
            if (!userChatRepository.existsByChat_ChatIdAndUser_UserId(chat.getChatId(), user.getUserId())) {
                userChatRepository.save(UserChat.builder()
                        .chat(chat)
                        .user(user)
                        .pinned(false)
                        .unread(false)
                        .muted(false)
                        .build());
            }
        }

        return dtoConversionService.mapToChatDTO(chat);
    }

    private ChatDTO rateLimitFallbackCreate(UUID user1Id, UUID user2Id, RequestNotPermitted t) {
        throw new RateLimitExceeded("Too many requests for chat creation; please retry later.");
    }

    @Override
    @RateLimiter(name = CHAT_RATE_LIMITER, fallbackMethod = "rateLimitFallbackGet")
    public List<ChatDTO> getChatsForUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFound("User not found with id: " + userId);
        }

        return userChatRepository.findByUser_UserId(userId).stream()
                .map(userChat -> dtoConversionService.mapToChatDTO(userChat.getChat()))
                .collect(Collectors.toList());
    }

    private List<ChatDTO> rateLimitFallbackGet(UUID userId, RequestNotPermitted t) {
        throw new RateLimitExceeded("Too many requests for fetching chats; please retry later.");
    }

    @Override
    @RateLimiter(name = CHAT_RATE_LIMITER, fallbackMethod = "rateLimitFallbackUpdate")
    public UserChatDTO partialUpdateUserChat(UUID chatId, UUID userId, UpdateUserChatDTO updateUserChatDTO) {
        return userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId).map(existingUserChat -> {
            Optional.of(updateUserChatDTO.isPinned()).ifPresent(existingUserChat::setPinned);
            Optional.of(updateUserChatDTO.isUnread()).ifPresent(existingUserChat::setUnread);
            Optional.of(updateUserChatDTO.isMuted()).ifPresent(existingUserChat::setMuted);

            UserChat updatedUserChat = userChatRepository.save(existingUserChat);
            return dtoConversionService.mapToUserChatDTO(updatedUserChat);
        }).orElseThrow(() -> new UserChatNotFound("User chat does not exist"));
    }

    private UserChatDTO rateLimitFallbackUpdate(UUID chatId, UUID userId, UpdateUserChatDTO updateUserChatDTO,
                                                RequestNotPermitted t) {
        throw new RateLimitExceeded("Too many update requests; please retry later.");
    }

    @Override
    @RateLimiter(name = CHAT_RATE_LIMITER, fallbackMethod = "rateLimitFallbackDelete")
    public void deleteUserChatById(UUID userChatId, UUID userId) {
        UserChat chat = userChatRepository.findById(userChatId)
                .orElseThrow(() -> new ChatNotFound("User chat does not exist"));
        if (!chat.getUser().getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only delete your chats");
        }

        userChatRepository.deleteById(userChatId);
    }

    private void rateLimitFallbackDelete(UUID userChatId, UUID userId, RequestNotPermitted t) {
        throw new RateLimitExceeded("Too many delete requests; please retry later.");
    }

    @Override
    public ChatDTO getChatById(UUID chatId, UUID userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFound("Chat does not exist"));
        if (!chat.getUser1().getUserId().equals(userId) &&
                !chat.getUser2().getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only get your chats");
        }

        return dtoConversionService.mapToChatDTO(chat);
    }
}
