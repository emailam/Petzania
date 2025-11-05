package com.example.friends.and.chats.module.service.impl;

import com.example.friends.and.chats.module.exception.chat.ChatNotFound;
import com.example.friends.and.chats.module.exception.chat.UserChatNotFound;
import com.example.friends.and.chats.module.exception.user.ForbiddenOperation;
import com.example.friends.and.chats.module.exception.user.UserAccessDenied;
import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.dto.chat.ChatDTO;
import com.example.friends.and.chats.module.model.dto.chat.UpdateUserChatDTO;
import com.example.friends.and.chats.module.model.dto.chat.UserChatDTO;
import com.example.friends.and.chats.module.model.dto.message.UnreadCountUpdateDTO;
import com.example.friends.and.chats.module.model.entity.Chat;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.entity.UserChat;
import com.example.friends.and.chats.module.repository.BlockRepository;
import com.example.friends.and.chats.module.repository.ChatRepository;
import com.example.friends.and.chats.module.repository.UserChatRepository;
import com.example.friends.and.chats.module.repository.UserRepository;
import com.example.friends.and.chats.module.service.IChatService;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class ChatService implements IChatService {
    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final IDTOConversionService dtoConversionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public ChatDTO createChatIfNotExists(UUID user1Id, UUID user2Id) {
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Cannot create a chat with yourself");
        }

        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new UserNotFound("User 1 not found"));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new UserNotFound("User 2 not found"));

        if (blockRepository.existsByBlockerAndBlocked(user1, user2) || blockRepository.existsByBlockerAndBlocked(user2, user1)) {
            throw new ForbiddenOperation("Operation Cannot be performed due to an existing block relationship");
        }

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
                        .unread(0)
                        .muted(false)
                        .build());
            }
        }

        return dtoConversionService.mapToChatDTO(chat);
    }

    @Override
    public List<ChatDTO> getChatsForUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFound("User not found with id: " + userId);
        }

        Sort sort = Sort.by(
                Sort.Order.desc("pinned"),
                Sort.Order.desc("chat.lastMessageTimestamp")
        );
        return userChatRepository
                .findByUser_UserId(userId, sort)
                .stream()
                .map(uc -> dtoConversionService.mapToChatDTO(uc.getChat()))
                .collect(Collectors.toList());
    }

    @Override
    public UserChatDTO partialUpdateUserChat(UUID chatId, UUID userId, UpdateUserChatDTO updateUserChatDTO) {
        return userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId).map(existingUserChat -> {
            Optional.ofNullable(updateUserChatDTO.getPinned()).ifPresent(existingUserChat::setPinned);
            Optional.ofNullable(updateUserChatDTO.getMuted()).ifPresent(existingUserChat::setMuted);

            UserChat updatedUserChat = userChatRepository.save(existingUserChat);
            return dtoConversionService.mapToUserChatDTO(updatedUserChat);
        }).orElseThrow(() -> new UserChatNotFound("User chat does not exist"));
    }


    @Override
    public void deleteUserChatById(UUID userChatId, UUID userId) {
        UserChat chat = userChatRepository.findById(userChatId)
                .orElseThrow(() -> new UserChatNotFound("User chat does not exist"));
        if (!chat.getUser().getUserId().equals(userId)) {
            throw new UserAccessDenied("You can only delete your chats");
        }
        int unreadCount = chat.getUnread();
        UUID chatId = chat.getChat().getChatId();

        userChatRepository.deleteById(userChatId);
        if (unreadCount > 0) {
            long totalUnread = userChatRepository.getTotalUnreadCount(userId);

            UnreadCountUpdateDTO unreadCountUpdateDTO = new UnreadCountUpdateDTO();
            unreadCountUpdateDTO.setTotalUnreadCount(totalUnread);
            unreadCountUpdateDTO.setUserChatUnreadCount(0);
            unreadCountUpdateDTO.setUserChatId(userChatId);

            messagingTemplate.convertAndSend(
                    "/topic/" + userId + "/unread-count",
                    unreadCountUpdateDTO
            );
        }
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

    @Override
    public UserChatDTO getUserChatById(UUID chatId, UUID userId) {
        UserChat userChat = userChatRepository
                .findByChat_ChatIdAndUser_UserId(chatId, userId)
                .orElseThrow(() -> new UserChatNotFound(
                        String.format("UserChat not found for chatId=%s and userId=%s", chatId, userId)
                ));

        return dtoConversionService.mapToUserChatDTO(userChat);
    }
}
