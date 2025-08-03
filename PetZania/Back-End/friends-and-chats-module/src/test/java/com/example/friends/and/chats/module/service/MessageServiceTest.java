package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.exception.chat.ChatNotFound;
import com.example.friends.and.chats.module.exception.message.*;
import com.example.friends.and.chats.module.exception.user.ForbiddenOperation;
import com.example.friends.and.chats.module.exception.user.UserAccessDenied;
import com.example.friends.and.chats.module.exception.user.UserNotFound;
import com.example.friends.and.chats.module.model.dto.message.*;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.model.enumeration.EventType;
import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.impl.MessageService;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private ChatRepository chatRepository;
    @Mock private UserChatRepository userChatRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private MessageReactionRepository messageReactionRepository;
    @Mock private IDTOConversionService dtoConversionService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendMessage_success() {
        UUID senderId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        User sender = new User(); sender.setUserId(senderId);
        Chat chat = new Chat(); chat.setChatId(chatId);
        // Set user1 and user2 on chat
        User user1 = new User(); user1.setUserId(senderId);
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        chat.setUser1(user1);
        chat.setUser2(user2);
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatId);
        sendMessageDTO.setContent("Hello");
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(blockRepository.existsByBlockerAndBlocked(any(), any())).thenReturn(false);
        Message message = new Message();
        message.setChat(chat);
        when(messageRepository.save(any())).thenReturn(message);
        MessageDTO messageDTO = new MessageDTO();
        when(dtoConversionService.mapToMessageDTO(message)).thenReturn(messageDTO);
        // Mock user chat access
        UserChat userChat = UserChat.builder().chat(chat).user(sender).build();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, senderId)).thenReturn(Optional.of(userChat));
        MessageDTO result = messageService.sendMessage(sendMessageDTO, senderId);
        assertNotNull(result);
    }

    @Test
    void sendMessage_userNotFound_throws() {
        UUID senderId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatId); // FIX: Set chatId to avoid NPE
        // Mock chatRepository to return a valid chat with users
        Chat chat = new Chat();
        chat.setChatId(chatId);
        User user1 = new User(); user1.setUserId(UUID.randomUUID());
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        chat.setUser1(user1);
        chat.setUser2(user2);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());
        assertThrows(UserNotFound.class, () -> messageService.sendMessage(sendMessageDTO, senderId));
    }

    @Test
    void sendMessage_chatNotFound_throws() {
        UUID senderId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        User sender = new User(); sender.setUserId(senderId);
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatId);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());
        assertThrows(ChatNotFound.class, () -> messageService.sendMessage(sendMessageDTO, senderId));
    }

    @Test
    void sendMessage_blocked_throws() {
        UUID senderId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        User sender = new User(); sender.setUserId(senderId);
        Chat chat = new Chat(); chat.setChatId(chatId); // FIX: Set chatId
        // Set user1 and user2 on chat
        User user1 = new User(); user1.setUserId(senderId);
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        chat.setUser1(user1);
        chat.setUser2(user2);
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatId); // FIX: Set chatId
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(blockRepository.existsByBlockerAndBlocked(any(), any())).thenReturn(true);
        assertThrows(ForbiddenOperation.class, () -> messageService.sendMessage(sendMessageDTO, senderId));
    }

    @Test
    void getMessagesByChat_success() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Chat chat = new Chat(); chat.setChatId(chatId);
        // Set user1 and user2 on chat
        User user1 = new User(); user1.setUserId(userId);
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        chat.setUser1(user1);
        chat.setUser2(user2);
        User user = new User(); user.setUserId(userId);
        Message message = new Message();
        message.setChat(chat);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageRepository.findByChat_ChatId(eq(chatId), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(message)));
        when(dtoConversionService.mapToMessageDTO(message)).thenReturn(new MessageDTO());
        // Mock user chat access
        UserChat userChat = UserChat.builder().chat(chat).user(user).build();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId)).thenReturn(Optional.of(userChat));
        var result = messageService.getMessagesByChat(chatId, userId, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMessagesByChat_chatNotFound_throws() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // Mock userChatRepository to return empty to trigger UserAccessDenied
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chatId, userId)).thenReturn(Optional.empty());
        assertThrows(UserAccessDenied.class, () -> messageService.getMessagesByChat(chatId, userId, 0, 10));
    }

    @Test
    void getMessagesByChat_userNotInChat_throws() {
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Chat chat = new Chat(); chat.setChatId(chatId);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(UserAccessDenied.class, () -> messageService.getMessagesByChat(chatId, userId, 0, 10));
    }

    @Test
    void getMessageById_success() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message message = new Message();
        message.setMessageId(messageId);
        message.setSender(new User());
        // FIX: Set the chat!
        Chat chat = new Chat();
        chat.setChatId(UUID.randomUUID());
        message.setChat(chat);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(dtoConversionService.mapToMessageDTO(message)).thenReturn(new MessageDTO());
        UserChat userChat = UserChat.builder().chat(chat).user(new User()).build();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(any(), any())).thenReturn(Optional.of(userChat));
        MessageDTO result = messageService.getMessageById(messageId, userId);
        assertNotNull(result);
    }

    @Test
    void getMessageById_notFound_throws() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        assertThrows(MessageNotFound.class, () -> messageService.getMessageById(messageId, userId));
    }

    @Test
    void deleteMessage_success() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message message = new Message();
        message.setMessageId(messageId);
        User sender = new User(); sender.setUserId(userId);
        message.setSender(sender);
        // FIX: Set the chat and its users!
        Chat chat = new Chat();
        chat.setChatId(UUID.randomUUID());
        User user1 = new User(); user1.setUserId(UUID.randomUUID());
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        chat.setUser1(user1);
        chat.setUser2(user2);
        message.setChat(chat);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        doNothing().when(messageRepository).deleteById(messageId);
        // Mock user chat access if needed
        UserChat userChat = UserChat.builder().chat(chat).user(sender).build();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chat.getChatId(), userId)).thenReturn(Optional.of(userChat));
        assertDoesNotThrow(() -> messageService.deleteMessage(messageId, userId));
        verify(messageRepository, times(1)).deleteById(messageId);
    }

    @Test
    void deleteMessage_notFound_throws() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        assertThrows(MessageNotFound.class, () -> messageService.deleteMessage(messageId, userId));
    }

    @Test
    void updateMessageContent_success() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message message = new Message();
        message.setMessageId(messageId);
        User sender = new User(); sender.setUserId(userId);
        message.setSender(sender);
        message.setStatus(MessageStatus.SENT);
        // Set the chat and its users!
        Chat chat = new Chat();
        chat.setChatId(UUID.randomUUID());
        User user1 = new User(); user1.setUserId(userId);
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        chat.setUser1(user1);
        chat.setUser2(user2);
        message.setChat(chat);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(message)).thenReturn(message);
        when(dtoConversionService.mapToMessageDTO(message)).thenReturn(new MessageDTO());
        // Mock user chat access
        UserChat userChat = UserChat.builder().chat(chat).user(sender).build();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chat.getChatId(), userId)).thenReturn(Optional.of(userChat));
        MessageDTO result = messageService.updateMessageContent(messageId, userId, "new content");
        assertNotNull(result);
    }

    @Test
    void updateMessageContent_notFound_throws() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        assertThrows(MessageNotFound.class, () -> messageService.updateMessageContent(messageId, userId, "content"));
    }

    @Test
    void updateMessageStatus_success() {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Message message = new Message();
        message.setMessageId(messageId);
        User sender = new User(); sender.setUserId(senderId);
        User receiver = new User(); receiver.setUserId(receiverId);
        message.setSender(sender);
        message.setStatus(MessageStatus.SENT);
        // Set the chat and its users!
        Chat chat = new Chat();
        chat.setChatId(UUID.randomUUID());
        chat.setUser1(sender);
        chat.setUser2(receiver);
        message.setChat(chat);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(message)).thenReturn(message);
        when(dtoConversionService.mapToMessageDTO(message)).thenReturn(new MessageDTO());
        // Mock user chat access for receiver
        UserChat userChat = UserChat.builder().chat(chat).user(receiver).build();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chat.getChatId(), receiverId)).thenReturn(Optional.of(userChat));
        MessageDTO result = messageService.updateMessageStatus(messageId, receiverId, MessageStatus.DELIVERED);
        assertNotNull(result);
    }

    @Test
    void updateMessageStatus_notFound_throws() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        assertThrows(MessageNotFound.class, () -> messageService.updateMessageStatus(messageId, userId, MessageStatus.READ));
    }

    @Test
    void reactToMessage_success() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message message = new Message();
        message.setMessageId(messageId);
        // Set the chat and its users!
        Chat chat = new Chat();
        chat.setChatId(UUID.randomUUID());
        User user1 = new User(); user1.setUserId(userId);
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        chat.setUser1(user1);
        chat.setUser2(user2);
        message.setChat(chat);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        UserChat userChat = UserChat.builder().chat(chat).user(new User()).build();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chat.getChatId(), userId)).thenReturn(Optional.of(userChat));
        MessageReaction reaction = new MessageReaction();
        when(messageReactionRepository.save(any())).thenReturn(reaction);
        MessageReactionDTO reactionDTO = new MessageReactionDTO();
        when(dtoConversionService.mapToMessageReactionDTO(reaction)).thenReturn(reactionDTO);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        MessageReactionDTO result = messageService.reactToMessage(messageId, userId, MessageReact.LIKE);
        assertNotNull(result);
    }

    @Test
    void reactToMessage_messageNotFound_throws() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        assertThrows(MessageNotFound.class, () -> messageService.reactToMessage(messageId, userId, MessageReact.LIKE));
    }

    @Test
    void getChatIdFromMessageId_success() {
        UUID messageId = UUID.randomUUID();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setChatId(UUID.randomUUID());
        message.setChat(chat);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        UUID result = messageService.getChatIdFromMessageId(messageId);
        assertNotNull(result);
    }

    @Test
    void getChatIdFromMessageId_notFound_throws() {
        UUID messageId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        assertThrows(MessageNotFound.class, () -> messageService.getChatIdFromMessageId(messageId));
    }

    @Test
    void removeReaction_success() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MessageReaction reaction = new MessageReaction();
        // FIX: Set message and chat with user1 and user2
        Message message = new Message();
        message.setMessageId(messageId);
        Chat chat = new Chat();
        chat.setChatId(UUID.randomUUID());
        User user1 = new User(); user1.setUserId(UUID.randomUUID());
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        chat.setUser1(user1);
        chat.setUser2(user2);
        message.setChat(chat);
        reaction.setMessage(message);
        when(messageReactionRepository.findByMessage_MessageIdAndUser_UserId(messageId, userId)).thenReturn(Optional.of(reaction));
        doNothing().when(messageReactionRepository).delete(reaction);
        // Mock user chat access if needed
        UserChat userChat = UserChat.builder().chat(chat).user(new User()).build();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chat.getChatId(), userId)).thenReturn(Optional.of(userChat));
        assertDoesNotThrow(() -> messageService.removeReaction(messageId, userId));
        verify(messageReactionRepository, times(1)).delete(reaction);
    }

    @Test
    void removeReaction_notFound_throws() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(messageReactionRepository.findByMessage_MessageIdAndUser_UserId(messageId, userId)).thenReturn(Optional.empty());
        assertThrows(MessageNotFound.class, () -> messageService.removeReaction(messageId, userId));
    }

    @Test
    void getReactionsForMessage_success() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message message = new Message();
        message.setMessageId(messageId);
        // Set the chat and its users!
        Chat chat = new Chat();
        chat.setChatId(UUID.randomUUID());
        User user1 = new User(); user1.setUserId(userId);
        User user2 = new User(); user2.setUserId(UUID.randomUUID());
        chat.setUser1(user1);
        chat.setUser2(user2);
        message.setChat(chat);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        MessageReaction reaction = new MessageReaction();
        when(messageReactionRepository.findByMessage_MessageId(messageId)).thenReturn(List.of(reaction));
        when(dtoConversionService.mapToMessageReactionDTO(reaction)).thenReturn(new MessageReactionDTO());
        // Mock user chat access
        UserChat userChat = UserChat.builder().chat(chat).user(new User()).build();
        when(userChatRepository.findByChat_ChatIdAndUser_UserId(chat.getChatId(), userId)).thenReturn(Optional.of(userChat));
        List<MessageReactionDTO> result = messageService.getReactionsForMessage(messageId, userId);
        assertEquals(1, result.size());
    }

    @Test
    void getReactionsForMessage_messageNotFound_throws() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        assertThrows(MessageNotFound.class, () -> messageService.getReactionsForMessage(messageId, userId));
    }
} 