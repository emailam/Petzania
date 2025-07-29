package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.exception.user.ForbiddenOperation;
import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.exception.user.UserAccessDenied;
import com.example.friends.and.chats.module.exception.chat.ChatNotFound;
import com.example.friends.and.chats.module.exception.chat.UserChatNotFound;
import com.example.friends.and.chats.module.model.dto.chat.ChatDTO;
import com.example.friends.and.chats.module.model.dto.chat.UserChatDTO;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.impl.ChatService;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {
    @Mock private ChatRepository chatRepository;
    @Mock private UserChatRepository userChatRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private UserRepository userRepository;
    @Mock private IDTOConversionService dtoConversionService;

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createChatIfNotExists_success() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        User user1 = new User(); user1.setUserId(user1Id);
        User user2 = new User(); user2.setUserId(user2Id);
        Chat chat = Chat.builder().user1(user1).user2(user2).build();
        ChatDTO chatDTO = new ChatDTO();

        when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));
        when(blockRepository.existsByBlockerAndBlocked(user1, user2)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(user2, user1)).thenReturn(false);
        when(chatRepository.findByUsers(user1, user2)).thenReturn(Optional.empty());
        when(chatRepository.save(any())).thenReturn(chat);
        when(dtoConversionService.mapToChatDTO(chat)).thenReturn(chatDTO);

        ChatDTO result = chatService.createChatIfNotExists(user1Id, user2Id);
        assertNotNull(result);
        verify(chatRepository, times(1)).save(any());
    }

    @Test
    void createChatIfNotExists_selfChat_throws() {
        UUID userId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> chatService.createChatIfNotExists(userId, userId));
    }

    @Test
    void createChatIfNotExists_userNotFound_throws() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        when(userRepository.findById(user1Id)).thenReturn(Optional.empty());
        assertThrows(UserNotFound.class, () -> chatService.createChatIfNotExists(user1Id, user2Id));
    }

    @Test
    void createChatIfNotExists_blocked_throws() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        User user1 = new User(); user1.setUserId(user1Id);
        User user2 = new User(); user2.setUserId(user2Id);
        when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));
        when(blockRepository.existsByBlockerAndBlocked(user1, user2)).thenReturn(true);
        assertThrows(ForbiddenOperation.class, () -> chatService.createChatIfNotExists(user1Id, user2Id));
    }

    @Test
    void createChatIfNotExists_alreadyExists_returnsExisting() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        User user1 = new User(); user1.setUserId(user1Id);
        User user2 = new User(); user2.setUserId(user2Id);
        Chat chat = Chat.builder().user1(user1).user2(user2).build();
        ChatDTO chatDTO = new ChatDTO();

        when(userRepository.findById(user1Id)).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2Id)).thenReturn(Optional.of(user2));
        when(blockRepository.existsByBlockerAndBlocked(user1, user2)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(user2, user1)).thenReturn(false);
        when(chatRepository.findByUsers(user1, user2)).thenReturn(Optional.of(chat));
        when(dtoConversionService.mapToChatDTO(chat)).thenReturn(chatDTO);

        ChatDTO result = chatService.createChatIfNotExists(user1Id, user2Id);
        assertNotNull(result);
        verify(chatRepository, never()).save(any());
    }

    @Test
    void getChatById_success() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user1 = new User(); user1.setUserId(userId);
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        Chat chat = Chat.builder().chatId(chatId).user1(user1).user2(user2).build();
        ChatDTO chatDTO = new ChatDTO();

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(dtoConversionService.mapToChatDTO(chat)).thenReturn(chatDTO);

        ChatDTO result = chatService.getChatById(chatId, userId);
        assertNotNull(result);
    }

    @Test
    void getChatById_notFound_throws() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());
        assertThrows(ChatNotFound.class, () -> chatService.getChatById(chatId, userId));
    }

    @Test
    void getChatById_accessDenied_throws() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user1 = new User(); user1.setUserId(UUID.randomUUID());
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        Chat chat = Chat.builder().chatId(chatId).user1(user1).user2(user2).build();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        assertThrows(UserAccessDenied.class, () -> chatService.getChatById(chatId, userId));
    }

    @Test
    void getUserChatById_success() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserChat userChat = UserChat.builder().build();
        UserChatDTO userChatDTO = new UserChatDTO();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId)).thenReturn(Optional.of(userChat));
        when(dtoConversionService.mapToUserChatDTO(userChat)).thenReturn(userChatDTO);
        UserChatDTO result = chatService.getUserChatById(chatId, userId);
        assertNotNull(result);
    }

    @Test
    void getUserChatById_notFound_throws() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId)).thenReturn(Optional.empty());
        assertThrows(UserChatNotFound.class, () -> chatService.getUserChatById(chatId, userId));
    }

    @Test
    void partialUpdateUserChat_success() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        com.example.friends.and.chats.module.model.dto.chat.UpdateUserChatDTO updateDTO = new com.example.friends.and.chats.module.model.dto.chat.UpdateUserChatDTO();
        UserChat userChat = UserChat.builder().build();
        UserChatDTO userChatDTO = new UserChatDTO();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId)).thenReturn(Optional.of(userChat));
        when(userChatRepository.save(userChat)).thenReturn(userChat);
        when(dtoConversionService.mapToUserChatDTO(userChat)).thenReturn(userChatDTO);
        UserChatDTO result = chatService.partialUpdateUserChat(chatId, userId, updateDTO);
        assertNotNull(result);
    }

    @Test
    void partialUpdateUserChat_notFound_throws() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        com.example.friends.and.chats.module.model.dto.chat.UpdateUserChatDTO updateDTO = new com.example.friends.and.chats.module.model.dto.chat.UpdateUserChatDTO();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId)).thenReturn(Optional.empty());
        assertThrows(UserChatNotFound.class, () -> chatService.partialUpdateUserChat(chatId, userId, updateDTO));
    }

    @Test
    void deleteUserChatById_success() {
        UUID userChatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User(); user.setUserId(userId);
        UserChat userChat = UserChat.builder().user(user).build();
        when(userChatRepository.findById(userChatId)).thenReturn(Optional.of(userChat));
        doNothing().when(userChatRepository).deleteById(userChatId);
        assertDoesNotThrow(() -> chatService.deleteUserChatById(userChatId, userId));
        verify(userChatRepository, times(1)).deleteById(userChatId);
    }

    @Test
    void deleteUserChatById_notFound_throws() {
        UUID userChatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userChatRepository.findById(userChatId)).thenReturn(Optional.empty());
        assertThrows(UserChatNotFound.class, () -> chatService.deleteUserChatById(userChatId, userId));
    }

    @Test
    void deleteUserChatById_accessDenied_throws() {
        UUID userChatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User(); user.setUserId(UUID.randomUUID());
        UserChat userChat = UserChat.builder().user(user).build();
        when(userChatRepository.findById(userChatId)).thenReturn(Optional.of(userChat));
        assertThrows(UserAccessDenied.class, () -> chatService.deleteUserChatById(userChatId, userId));
    }

    @Test
    void getChatById_user2Access_success() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user1 = new User(); user1.setUserId(UUID.randomUUID());
        User user2 = new User(); user2.setUserId(userId);
        Chat chat = Chat.builder().chatId(chatId).user1(user1).user2(user2).build();
        ChatDTO chatDTO = new ChatDTO();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(dtoConversionService.mapToChatDTO(chat)).thenReturn(chatDTO);
        ChatDTO result = chatService.getChatById(chatId, userId);
        assertNotNull(result);
    }

    @Test
    void getUserChatById_userNotFound_throws() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId)).thenReturn(Optional.empty());
        assertThrows(UserChatNotFound.class, () -> chatService.getUserChatById(chatId, userId));
    }
} 