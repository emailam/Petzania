package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.TestDataUtil;
import com.example.friends.and.chats.module.model.dto.message.SendMessageDTO;
import com.example.friends.and.chats.module.model.dto.message.UpdateMessageContentDTO;
import com.example.friends.and.chats.module.model.dto.message.UpdateMessageReactDTO;
import com.example.friends.and.chats.module.model.dto.message.UpdateMessageStatusDTO;
import com.example.friends.and.chats.module.model.dto.message.UnreadCountUpdateDTO;
import com.example.friends.and.chats.module.model.entity.Chat;
import com.example.friends.and.chats.module.model.entity.Message;
import com.example.friends.and.chats.module.model.entity.MessageReaction;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.entity.UserChat;
import com.example.friends.and.chats.module.model.enumeration.MessageReact;
import com.example.friends.and.chats.module.model.enumeration.MessageStatus;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Transactional
public class MessageControllerIntegrationTests {
    public static class TestSimpMessagingTemplate extends SimpMessagingTemplate {
        private List<String> sentDestinations = new ArrayList<>();
        private List<Object> sentPayloads = new ArrayList<>();

        public TestSimpMessagingTemplate() {
            super(new AbstractMessageChannel() {
                @Override
                protected boolean sendInternal(org.springframework.messaging.Message message, long timeout) {
                    return true;
                }
            });
        }

        @Override
        public void convertAndSend(String destination, Object payload) {
            sentDestinations.add(destination);
            sentPayloads.add(payload);
        }

        public List<String> getSentDestinations() {
            return sentDestinations;
        }

        public List<Object> getSentPayloads() {
            return sentPayloads;
        }

        public void reset() {
            sentDestinations.clear();
            sentPayloads.clear();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private TestSimpMessagingTemplate getTestMessagingTemplate() {
        return (TestSimpMessagingTemplate) messagingTemplate;
    }

    @TestConfiguration
    @Order(Ordered.HIGHEST_PRECEDENCE)
    static class TestConfig {
        @Bean
        @Primary
        public SimpMessagingTemplate testSimpMessagingTemplate() {
            return new MessageControllerIntegrationTests.TestSimpMessagingTemplate();
        }
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserChatRepository userChatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReactionRepository messageReactionRepository;

    @Autowired
    private MessageController messageController;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final int RATE_LIMIT_VALUE = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User userA;
    private User userB;
    private User userC;
    private Chat chatAB;
    private Chat chatBC;
    private UserChat userAChatAB;
    private UserChat userBChatAB;
    private UserChat userBChatBC;
    private UserChat userCChatBC;
    private Message messageFromA;
    private Message messageFromB;
    private Message replyMessage;

    @BeforeEach
    void setup() {
        // Clean up first
        messageReactionRepository.deleteAll();
        messageRepository.deleteAll();
        userChatRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        userA = userRepository.save(TestDataUtil.createTestUser("userA"));
        userB = userRepository.save(TestDataUtil.createTestUser("userB"));
        userC = userRepository.save(TestDataUtil.createTestUser("userC"));

        // Set up security context for userA
        setupSecurityContext(userA);


        // Set up a chat between userA and userB
        chatAB = chatRepository.save(Chat.builder()
                .user1(userA)
                .user2(userB)
                .build());

        chatBC = chatRepository.save(Chat.builder()
                .user1(userB)
                .user2(userC)
                .build());

        // Create UserChat entries
        userAChatAB = userChatRepository.save(UserChat.builder()
                .chat(chatAB)
                .user(userA)
                .pinned(false)
                .unread(0)
                .muted(false)
                .build());

        userBChatAB = userChatRepository.save(UserChat.builder()
                .chat(chatAB)
                .user(userB)
                .pinned(false)
                .unread(0)
                .muted(false)
                .build());

        userBChatBC = userChatRepository.save(UserChat.builder()
                .chat(chatBC)
                .user(userB)
                .pinned(false)
                .unread(0)
                .muted(false)
                .build());

        userCChatBC = userChatRepository.save(UserChat.builder()
                .chat(chatBC)
                .user(userC)
                .pinned(false)
                .unread(0)
                .muted(false)
                .build());

        // Create some messages
        messageFromA = messageRepository.save(Message.builder()
                .chat(chatAB)
                .sender(userA)
                .content("Hello from A")
                .status(MessageStatus.SENT)
                .isFile(false)
                .isEdited(false)
                .sentAt(LocalDateTime.now().minusMinutes(10))
                .build());

        messageFromB = messageRepository.save(Message.builder()
                .chat(chatAB)
                .sender(userB)
                .content("Hello from B")
                .status(MessageStatus.SENT)
                .isFile(false)
                .isEdited(false)
                .sentAt(LocalDateTime.now().minusMinutes(5))
                .build());

        // Create a reply message
        replyMessage = messageRepository.save(Message.builder()
                .chat(chatAB)
                .sender(userA)
                .content("This is a reply")
                .replyTo(messageFromB)
                .status(MessageStatus.SENT)
                .isFile(false)
                .isEdited(false)
                .sentAt(LocalDateTime.now())
                .build());
    }

    private void setupSecurityContext(User user) {
        UserPrincipal userPrincipal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                )
        );
    }

    @AfterEach
    void tearDown() {
        // Clear the security context after each test
        SecurityContextHolder.clearContext();
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (!rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
    }

    @Test
    void testDeleteUserChat_WithUnreadMessages_UpdatesTotalUnreadCount() throws Exception {
        // Setup: Create multiple chats with unread messages
        // Chat AB already exists from setup

        // Create another chat between userA and userC
        Chat chatAC = chatRepository.save(Chat.builder()
                .user1(userA)
                .user2(userC)
                .build());

        UserChat userAChatAC = userChatRepository.save(UserChat.builder()
                .chat(chatAC)
                .user(userA)
                .pinned(false)
                .unread(0)
                .muted(false)
                .build());

        UserChat userCChatAC = userChatRepository.save(UserChat.builder()
                .chat(chatAC)
                .user(userC)
                .pinned(false)
                .unread(0)
                .muted(false)
                .build());

        // Reset WebSocket template to track new messages
        TestSimpMessagingTemplate testTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testTemplate.reset();

        // Send messages to userA in both chats
        // 3 messages from B to A in chatAB
        for (int i = 1; i <= 3; i++) {
            SendMessageDTO msg = new SendMessageDTO();
            msg.setChatId(chatAB.getChatId());
            msg.setContent("Message from B " + i);
            msg.setFile(false);

            mockMvc.perform(post("/api/messages/send")
                            .with(user(new UserPrincipal(userB)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(msg)))
                    .andExpect(status().isOk());
        }
        entityManager.flush();
        entityManager.clear();

        // Verify WebSocket notification was sent with updated count
        List<String> destinations = testTemplate.getSentDestinations();
        List<Object> payloads = testTemplate.getSentPayloads();
        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userA.getUserId() + "/unread-count"));
        int index = destinations.lastIndexOf("/topic/" + userA.getUserId() + "/unread-count");
        UnreadCountUpdateDTO unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(3L, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(3, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userAChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // 2 messages from C to A in chatAC
        for (int i = 1; i <= 2; i++) {
            SendMessageDTO msg = new SendMessageDTO();
            msg.setChatId(chatAC.getChatId());
            msg.setContent("Message from C " + i);
            msg.setFile(false);

            mockMvc.perform(post("/api/messages/send")
                            .with(user(new UserPrincipal(userC)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(msg)))
                    .andExpect(status().isOk());
        }
        entityManager.flush();
        entityManager.clear();

        destinations = testTemplate.getSentDestinations();
        payloads = testTemplate.getSentPayloads();
        assertTrue(destinations.contains("/topic/" + userA.getUserId() + "/unread-count"));
        index = destinations.lastIndexOf("/topic/" + userA.getUserId() + "/unread-count");
        unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(5L, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(2, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userAChatAC.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // Verify userA has 5 total unread messages (3 from B + 2 from C)
        mockMvc.perform(get("/api/messages/unread-count")
                        .with(user(new UserPrincipal(userA))))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        // Get the updated userAChatAB to get its ID
        userAChatAB = userChatRepository.findByChat_ChatIdAndUser_UserId(
                chatAB.getChatId(), userA.getUserId()).orElseThrow();


        // Act: Delete userA's chat with B (which has 3 unread messages)
        mockMvc.perform(delete("/api/chats/user/{userChatId}", userAChatAB.getUserChatId())
                        .with(user(new UserPrincipal(userA))))
                .andExpect(status().isNoContent());

        // Assert: Total unread count should now be 2 (only messages from C remain)
        mockMvc.perform(get("/api/messages/unread-count")
                        .with(user(new UserPrincipal(userA))))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));


        // Should have received an unread-count update
        destinations = testTemplate.getSentDestinations();
        payloads = testTemplate.getSentPayloads();
        assertTrue(destinations.contains("/topic/" + userA.getUserId() + "/unread-count"));
        index = destinations.lastIndexOf("/topic/" + userA.getUserId() + "/unread-count");
        unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(2L, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(0, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userAChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // Verify the UserChat is actually deleted
        assertFalse(userChatRepository.existsById(userAChatAB.getUserChatId()));

        // Verify chat with C still exists and has correct unread count
        UserChat remainingChat = userChatRepository.findByChat_ChatIdAndUser_UserId(
                chatAC.getChatId(), userA.getUserId()).orElseThrow();
        assertEquals(2, remainingChat.getUnread());
    }

    @Test
    void testDeleteUserChat_WithNoUnreadMessages_NoUnreadCountNotification() throws Exception {
        // Setup: UserA has no unread messages in chatAB
        // (Initial state from setup has 0 unread)

        // Reset WebSocket template
        TestSimpMessagingTemplate testTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testTemplate.reset();

        // Act: Delete userA's chat with B (which has 0 unread messages)
        mockMvc.perform(delete("/api/chats/user/{userChatId}", userAChatAB.getUserChatId())
                        .with(user(new UserPrincipal(userA))))
                .andExpect(status().isNoContent());

        // Assert: No unread-count notification should be sent
        List<String> destinations = testTemplate.getSentDestinations();
        assertFalse(destinations.contains("/topic/" + userA.getUserId() + "/unread-count"),
                "Should not send unread count notification when deleting chat with 0 unread messages");

        // Verify total unread count is still 0
        mockMvc.perform(get("/api/messages/unread-count")
                        .with(user(new UserPrincipal(userA))))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void testDeleteUserChat_OtherUserUnaffected() throws Exception {
        // Reset WebSocket template
        TestSimpMessagingTemplate testTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testTemplate.reset();

        // Setup: Both users have unread messages
        // Send message from A to B
        mockMvc.perform(post("/api/messages/send")
                        .with(user(new UserPrincipal(userA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                SendMessageDTO.builder()
                                        .chatId(chatAB.getChatId())
                                        .content("From A to B")
                                        .isFile(false)
                                        .build())))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        // Verify WebSocket notification was sent with updated count
        List<String> destinations = testTemplate.getSentDestinations();
        List<Object> payloads = testTemplate.getSentPayloads();
        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userB.getUserId() + "/unread-count"));
        int index = destinations.lastIndexOf("/topic/" + userB.getUserId() + "/unread-count");
        UnreadCountUpdateDTO unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(1L, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(1, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userBChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // Send message from B to A
        mockMvc.perform(post("/api/messages/send")
                        .with(user(new UserPrincipal(userB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                SendMessageDTO.builder()
                                        .chatId(chatAB.getChatId())
                                        .content("From B to A")
                                        .isFile(false)
                                        .build())))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        // Verify WebSocket notification was sent with updated count
        destinations = testTemplate.getSentDestinations();
        payloads = testTemplate.getSentPayloads();
        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userA.getUserId() + "/unread-count"));
        index = destinations.lastIndexOf("/topic/" + userA.getUserId() + "/unread-count");
        unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(1L, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(1, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userAChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // Verify both have 1 unread message
        assertEquals(1, userChatRepository.findById(userAChatAB.getUserChatId()).orElseThrow().getUnread());
        assertEquals(1, userChatRepository.findById(userBChatAB.getUserChatId()).orElseThrow().getUnread());

        // Act: UserA deletes their chat
        mockMvc.perform(delete("/api/chats/user/{userChatId}", userAChatAB.getUserChatId())
                        .with(user(new UserPrincipal(userA))))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        // Verify WebSocket notification was sent with updated count
        destinations = testTemplate.getSentDestinations();
        payloads = testTemplate.getSentPayloads();
        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userA.getUserId() + "/unread-count"));
        index = destinations.lastIndexOf("/topic/" + userA.getUserId() + "/unread-count");
        unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(0L, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(0, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userAChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // Assert: UserB's unread count is unaffected
        UserChat userBChat = userChatRepository.findById(userBChatAB.getUserChatId()).orElseThrow();
        assertEquals(1, userBChat.getUnread(), "UserB's unread count should remain unchanged");

        // UserB can still see their total unread count
        mockMvc.perform(get("/api/messages/unread-count")
                        .with(user(new UserPrincipal(userB))))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    void testSendMessage_IncreasesReceiverUnreadCount() throws Exception {
        // Setup: userA sends to userB
        setupSecurityContext(userA);

        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatAB.getChatId());
        sendMessageDTO.setContent("Hello B!");
        sendMessageDTO.setFile(false);

        // Reset WebSocket template
        TestSimpMessagingTemplate testTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testTemplate.reset();

        // Act
        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        // Verify WebSocket notification was sent with updated count
        List<String> destinations = testTemplate.getSentDestinations();
        List<Object> payloads = testTemplate.getSentPayloads();

        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userB.getUserId() + "/unread-count"));
        int index = destinations.lastIndexOf("/topic/" + userB.getUserId() + "/unread-count");
        UnreadCountUpdateDTO unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(1L, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(1, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userBChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // Assert: Check userB's unread count
        UserChat updatedUserBChat = userChatRepository.findByChat_ChatIdAndUser_UserId(
                chatAB.getChatId(), userB.getUserId()).orElseThrow();
        assertEquals(1, updatedUserBChat.getUnread());
    }

    @Test
    void testMultipleMessages_CorrectlyIncrementsUnreadCount() throws Exception {
        // Setup: userA sends multiple messages to userB
        setupSecurityContext(userA);

        // Reset WebSocket template
        TestSimpMessagingTemplate testTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testTemplate.reset();

        // Send 6 messages
        for (int i = 1; i <= 6; i++) {
            SendMessageDTO sendMessageDTO = new SendMessageDTO();
            sendMessageDTO.setChatId(chatAB.getChatId());
            sendMessageDTO.setContent("Message " + i);
            sendMessageDTO.setFile(false);

            mockMvc.perform(post("/api/messages/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sendMessageDTO)))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.clear();

            // Verify WebSocket notification was sent with updated count
            List<String> destinations = testTemplate.getSentDestinations();
            List<Object> payloads = testTemplate.getSentPayloads();

            // Should have received an unread-count update
            assertTrue(destinations.contains("/topic/" + userB.getUserId() + "/unread-count"));
            int index = destinations.lastIndexOf("/topic/" + userB.getUserId() + "/unread-count");
            UnreadCountUpdateDTO unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
            assertEquals(i, unreadCountUpdateDTO.getTotalUnreadCount());
            assertEquals(i, unreadCountUpdateDTO.getUserChatUnreadCount());
            assertEquals(userBChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());
        }
        // Assert: userB should have 6 unread messages
        UserChat updatedUserBChat = userChatRepository.findByChat_ChatIdAndUser_UserId(
                chatAB.getChatId(), userB.getUserId()).orElseThrow();
        assertEquals(6, updatedUserBChat.getUnread());

        // Get total unread count for userB
        mockMvc.perform(get("/api/messages/unread-count").with(user(new UserPrincipal(userB))))
                .andExpect(status().isOk())
                .andExpect(content().string("6"));
    }

    @Test
    void testReadMessage_DecreasesUnreadCount() throws Exception {
        // Setup: userA sends message to userB
        setupSecurityContext(userA);
        Message message = messageRepository.save(Message.builder()
                .chat(chatAB)
                .sender(userA)
                .content("Unread message")
                .status(MessageStatus.SENT)
                .isFile(false)
                .isEdited(false)
                .build());

        // Reset WebSocket template
        TestSimpMessagingTemplate testTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testTemplate.reset();


        // Manually increment unread count (simulating what would happen)
        userBChatAB.setUnread(1);
        userChatRepository.save(userBChatAB);


        UpdateMessageStatusDTO statusUpdate = new UpdateMessageStatusDTO();

        statusUpdate.setMessageStatus(MessageStatus.DELIVERED);
        mockMvc.perform(patch("/api/messages/{messageId}/status", message.getMessageId()).with(user(new UserPrincipal(userB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk());

        statusUpdate.setMessageStatus(MessageStatus.READ);
        mockMvc.perform(patch("/api/messages/{messageId}/status", message.getMessageId()).with(user(new UserPrincipal(userB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        // Assert: unread count decreased
        UserChat updatedUserBChat = userChatRepository.findById(userBChatAB.getUserChatId()).orElseThrow();
        assertEquals(0, updatedUserBChat.getUnread());

        // Verify WebSocket notification was sent with updated count
        List<String> destinations = testTemplate.getSentDestinations();
        List<Object> payloads = testTemplate.getSentPayloads();

        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userB.getUserId() + "/unread-count"));
        int index = destinations.lastIndexOf("/topic/" + userB.getUserId() + "/unread-count");
        UnreadCountUpdateDTO unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(0, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(0, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userBChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());
    }

    @Test
    void testDeleteUnreadMessage_DecreasesReceiverUnreadCount() throws Exception {
        // Setup: userA sends message to userB
        setupSecurityContext(userA);
        Message message = messageRepository.save(Message.builder()
                .chat(chatAB)
                .sender(userA)
                .content("Message to delete")
                .status(MessageStatus.SENT) // Not READ
                .isFile(false)
                .isEdited(false)
                .build());

        // Manually set unread count
        userBChatAB.setUnread(1);
        userChatRepository.save(userBChatAB);

        // Act: userA deletes the unread message
        TestSimpMessagingTemplate testSimpMessagingTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testSimpMessagingTemplate.reset();
        mockMvc.perform(delete("/api/messages/{messageId}", message.getMessageId()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        // Assert: userB's unread count decreased
        UserChat updatedUserBChat = userChatRepository.findById(userBChatAB.getUserChatId()).orElseThrow();
        assertEquals(0, updatedUserBChat.getUnread());

        // Verify WebSocket notification was sent with updated count
        List<String> destinations = testSimpMessagingTemplate.getSentDestinations();
        List<Object> payloads = testSimpMessagingTemplate.getSentPayloads();

        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userB.getUserId() + "/unread-count"));
        int index = destinations.lastIndexOf("/topic/" + userB.getUserId() + "/unread-count");
        UnreadCountUpdateDTO unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(0, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(0, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userBChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());
    }

    @Test
    void testDeleteReadMessage_DoesNotChangeUnreadCount() throws Exception {
        // Setup: message already read
        setupSecurityContext(userA);
        Message message = messageRepository.save(Message.builder()
                .chat(chatAB)
                .sender(userA)
                .content("Already read message")
                .status(MessageStatus.READ) // Already READ
                .isFile(false)
                .isEdited(false)
                .build());

        // unread count is 0
        assertEquals(0, userBChatAB.getUnread());

        // Act: delete the read message
        TestSimpMessagingTemplate testSimpMessagingTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testSimpMessagingTemplate.reset();

        mockMvc.perform(delete("/api/messages/{messageId}", message.getMessageId()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        // Assert: unread count still 0
        UserChat updatedUserBChat = userChatRepository.findById(userBChatAB.getUserChatId()).orElseThrow();
        assertEquals(0, updatedUserBChat.getUnread());

        // Assert: No unread count notification sent
        assertFalse(testSimpMessagingTemplate.getSentDestinations().contains("/topic/" + userB.getUserId() + "/unread-count"));
    }

    @Test
    void testTotalUnreadCount_AcrossMultipleChats() throws Exception {
        // Setup: userB has unread messages in multiple chats
        setupSecurityContext(userA);

        // Reset WebSocket template
        TestSimpMessagingTemplate testTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testTemplate.reset();

        // Send 2 messages in chatAB
        for (int i = 0; i < 2; i++) {
            SendMessageDTO msg = new SendMessageDTO();
            msg.setChatId(chatAB.getChatId());
            msg.setContent("Message from A");
            msg.setFile(false);

            mockMvc.perform(post("/api/messages/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(msg)))
                    .andExpect(status().isOk());
        }
        entityManager.flush();
        entityManager.clear();

        // Verify WebSocket notification was sent with updated count
        List<String> destinations = testTemplate.getSentDestinations();
        List<Object> payloads = testTemplate.getSentPayloads();

        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userB.getUserId() + "/unread-count"));
        int index = destinations.lastIndexOf("/topic/" + userB.getUserId() + "/unread-count");
        UnreadCountUpdateDTO unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(2L, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(2, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userBChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // Send 3 messages in chatBC (from userC to userB)
        setupSecurityContext(userC);
        for (int i = 0; i < 3; i++) {
            SendMessageDTO msg = new SendMessageDTO();
            msg.setChatId(chatBC.getChatId());
            msg.setContent("Message from C");
            msg.setFile(false);

            mockMvc.perform(post("/api/messages/send")
                            .with(user(new UserPrincipal(userC)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(msg)))
                    .andExpect(status().isOk());
        }
        entityManager.flush();
        entityManager.clear();

        // Verify WebSocket notification was sent with updated count
        destinations = testTemplate.getSentDestinations();
        payloads = testTemplate.getSentPayloads();

        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userB.getUserId() + "/unread-count"));
        index = destinations.lastIndexOf("/topic/" + userB.getUserId() + "/unread-count");
        unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(5L, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(3, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userBChatBC.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // Assert: userB has total 5 unread messages
        mockMvc.perform(get("/api/messages/unread-count")
                        .with(user(new UserPrincipal(userB))))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void testMessageStatusTransitions_OnlyREADDecreasesCount() throws Exception {
        // Setup: unread message
        setupSecurityContext(userA);
        Message message = messageRepository.save(Message.builder()
                .chat(chatAB)
                .sender(userA)
                .content("Test status transitions")
                .status(MessageStatus.SENT)
                .isFile(false)
                .isEdited(false)
                .build());

        userBChatAB.setUnread(1);
        userChatRepository.save(userBChatAB);

        // Act 1: Change to DELIVERED (should NOT decrease count)
        UpdateMessageStatusDTO statusUpdate = new UpdateMessageStatusDTO();
        statusUpdate.setMessageStatus(MessageStatus.DELIVERED);

        mockMvc.perform(patch("/api/messages/{messageId}/status", message.getMessageId())
                        .with(user(new UserPrincipal(userB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk());

        // Assert: unread count still 1
        UserChat chat1 = userChatRepository.findById(userBChatAB.getUserChatId()).orElseThrow();
        assertEquals(1, chat1.getUnread());

        // Act 2: Change to READ (should decrease count)
        statusUpdate.setMessageStatus(MessageStatus.READ);

        // Reset WebSocket template
        TestSimpMessagingTemplate testTemplate = (TestSimpMessagingTemplate) messagingTemplate;
        testTemplate.reset();

        mockMvc.perform(patch("/api/messages/{messageId}/status", message.getMessageId())
                        .with(user(new UserPrincipal(userB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        // Verify WebSocket notification was sent with updated count
        List<String> destinations = testTemplate.getSentDestinations();
        List<Object> payloads = testTemplate.getSentPayloads();

        // Should have received an unread-count update
        assertTrue(destinations.contains("/topic/" + userB.getUserId() + "/unread-count"));
        int index = destinations.lastIndexOf("/topic/" + userB.getUserId() + "/unread-count");
        UnreadCountUpdateDTO unreadCountUpdateDTO = (UnreadCountUpdateDTO) payloads.get(index);
        assertEquals(0, unreadCountUpdateDTO.getTotalUnreadCount());
        assertEquals(0, unreadCountUpdateDTO.getUserChatUnreadCount());
        assertEquals(userBChatAB.getUserChatId(), unreadCountUpdateDTO.getUserChatId());

        // Assert: unread count now 0
        UserChat chat2 = userChatRepository.findById(userBChatAB.getUserChatId()).orElseThrow();
        assertEquals(0, chat2.getUnread());
    }

    @Test
    void sendMessage_Success() throws Exception {
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatAB.getChatId());
        sendMessageDTO.setContent("Test message");
        sendMessageDTO.setFile(false);

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Test message"))
                .andExpect(jsonPath("$.senderId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.chatId").value(chatAB.getChatId().toString()))
                .andExpect(jsonPath("$.status").value("SENT"));

        entityManager.flush();
        entityManager.clear();

        // Verify message was saved
        assertEquals(4, messageRepository.count());
    }

    @Test
    void testRateLimit_sendMessage() throws Exception {
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatAB.getChatId());
        sendMessageDTO.setContent("Test message");
        sendMessageDTO.setFile(false);

        for (int i = 0; i < RATE_LIMIT_VALUE; i++) {
            mockMvc.perform(post("/api/messages/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sendMessageDTO)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void sendMessage_WithReply_Success() throws Exception {
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatAB.getChatId());
        sendMessageDTO.setContent("Reply to message");
        sendMessageDTO.setFile(false);
        sendMessageDTO.setReplyToMessageId(messageFromB.getMessageId());

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Reply to message"))
                .andExpect(jsonPath("$.replyToMessageId").value(messageFromB.getMessageId().toString()));

        entityManager.flush();
        entityManager.clear();

        // Verify message was saved with reply reference
        assertEquals(4, messageRepository.count());
    }

    @Test
    void sendMessage_NonExistentChat_ShouldFail() throws Exception {
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(UUID.randomUUID());
        sendMessageDTO.setContent("Test message");

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendMessage_NonExistentReplyMessage_ShouldFail() throws Exception {
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatAB.getChatId());
        sendMessageDTO.setContent("Reply to non-existent message");
        sendMessageDTO.setReplyToMessageId(UUID.randomUUID());

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessagesByChat_Success() throws Exception {
        mockMvc.perform(get("/api/messages/chat/{chatId}", chatAB.getChatId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void testRateLimit_getMessagesByChat() throws Exception {
        for (int i = 0; i < RATE_LIMIT_VALUE; i++) {
            mockMvc.perform(get("/api/messages/chat/{chatId}", chatAB.getChatId()))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/messages/chat/{chatId}", chatAB.getChatId()))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void getMessagesByChat_WithPagination_Success() throws Exception {
        // Add more messages to test pagination
        for (int i = 0; i < 5; i++) {
            messageRepository.save(Message.builder()
                    .chat(chatAB)
                    .sender(userA)
                    .content("Pagination test " + i)
                    .status(MessageStatus.SENT)
                    .isFile(false)
                    .isEdited(false)
                    .sentAt(LocalDateTime.now().plusSeconds(i))
                    .build());
        }

        mockMvc.perform(get("/api/messages/chat/{chatId}", chatAB.getChatId())
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/messages/chat/{chatId}", chatAB.getChatId())
                        .param("page", "1")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));

        mockMvc.perform(get("/api/messages/chat/{chatId}", chatAB.getChatId())
                        .param("page", "2")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void getMessageById_Success() throws Exception {
        mockMvc.perform(get("/api/messages/{messageId}", messageFromA.getMessageId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageFromA.getMessageId().toString()))
                .andExpect(jsonPath("$.content").value("Hello from A"));
    }

    @Test
    void getMessageById_NonExistentMessage_ShouldFail() throws Exception {
        mockMvc.perform(get("/api/messages/{messageId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMessageContent_Success() throws Exception {
        UpdateMessageContentDTO updateDTO = new UpdateMessageContentDTO();
        updateDTO.setContent("Updated content");

        mockMvc.perform(patch("/api/messages/{messageId}/content", messageFromA.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.edited").value(true));

        // Verify message was updated
        Message updatedMessage = messageRepository.findById(messageFromA.getMessageId()).orElseThrow();
        assertEquals("Updated content", updatedMessage.getContent());
        assertTrue(updatedMessage.isEdited());
    }

    @Test
    void updateMessageContent_FileMessage_ShouldFail() throws Exception {
        // Create a file message
        Message fileMessage = messageRepository.save(Message.builder()
                .chat(chatAB)
                .sender(userA)
                .content("file://test.jpg")
                .status(MessageStatus.SENT)
                .isFile(true)
                .isEdited(false)
                .sentAt(LocalDateTime.now())
                .build());

        UpdateMessageContentDTO updateDTO = new UpdateMessageContentDTO();
        updateDTO.setContent("Trying to update file message");

        mockMvc.perform(patch("/api/messages/{messageId}/content", fileMessage.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest());

        // Verify message was not updated
        Message unchangedMessage = messageRepository.findById(fileMessage.getMessageId()).orElseThrow();
        assertEquals("file://test.jpg", unchangedMessage.getContent());
        assertFalse(unchangedMessage.isEdited());
    }

    @Test
    void updateMessageContent_MessageFromOtherUser_ShouldFail() throws Exception {
        UpdateMessageContentDTO updateDTO = new UpdateMessageContentDTO();
        updateDTO.setContent("Trying to update someone else's message");

        mockMvc.perform(patch("/api/messages/{messageId}/content", messageFromB.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden());

        // Verify message was not updated
        Message unchangedMessage = messageRepository.findById(messageFromB.getMessageId()).orElseThrow();
        assertEquals("Hello from B", unchangedMessage.getContent());
    }

    @Test
    void updateMessageStatus_BySender_ShouldFail() throws Exception {
        UpdateMessageStatusDTO updateDTO = new UpdateMessageStatusDTO();
        updateDTO.setMessageStatus(MessageStatus.DELIVERED);

        mockMvc.perform(patch("/api/messages/{messageId}/status", messageFromA.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reactToMessage_Success() throws Exception {
        UpdateMessageReactDTO updateDTO = new UpdateMessageReactDTO();
        updateDTO.setMessageReact(MessageReact.LIKE);

        mockMvc.perform(put("/api/messages/{messageId}/reaction", messageFromB.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.reactionType").value("LIKE"));

        // Verify reaction was saved
        assertEquals(1, messageReactionRepository.count());

        Optional<MessageReaction> savedReaction = messageReactionRepository.findByMessage_MessageIdAndUser_UserId(
                messageFromB.getMessageId(), userA.getUserId());
        assertTrue(savedReaction.isPresent());
        assertEquals(MessageReact.LIKE, savedReaction.get().getReactionType());
    }

    @Test
    void testRateLimit_reactToMessage() throws Exception {
        UpdateMessageReactDTO updateDTO = new UpdateMessageReactDTO();
        updateDTO.setMessageReact(MessageReact.LIKE);
        for (int i = 0; i < RATE_LIMIT_VALUE; i++) {
            mockMvc.perform(put("/api/messages/{messageId}/reaction", messageFromB.getMessageId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(put("/api/messages/{messageId}/reaction", messageFromB.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void reactToMessage_UpdateExistingReaction_Success() throws Exception {
        // First reaction
        UpdateMessageReactDTO updateDTO = new UpdateMessageReactDTO();
        updateDTO.setMessageReact(MessageReact.LIKE);

        mockMvc.perform(put("/api/messages/{messageId}/reaction", messageFromB.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());

        // Update to a different reaction
        updateDTO.setMessageReact(MessageReact.LOVE);

        mockMvc.perform(put("/api/messages/{messageId}/reaction", messageFromB.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactionType").value("LOVE"));

        // Verify only one reaction exists
        assertEquals(1, messageReactionRepository.count());
        Optional<MessageReaction> updatedReaction = messageReactionRepository.findByMessage_MessageIdAndUser_UserId(
                messageFromB.getMessageId(), userA.getUserId());
        assertTrue(updatedReaction.isPresent());
        assertEquals(MessageReact.LOVE, updatedReaction.get().getReactionType());
    }

    @Test
    void sendMessage_WithEmptyContent_ShouldSuccess() throws Exception {
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatAB.getChatId());
        sendMessageDTO.setContent("");
        sendMessageDTO.setFile(false);

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void sendMessage_WithLongContent_Success() throws Exception {
        // Create a message with 1000 characters
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("a");
        }

        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatAB.getChatId());
        sendMessageDTO.setContent(longContent.toString());
        sendMessageDTO.setFile(false);

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(longContent.toString()));
    }

    @Test
    void sendFileMessage_Success() throws Exception {
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatAB.getChatId());
        sendMessageDTO.setContent("file://test_image.jpg");
        sendMessageDTO.setFile(true);

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.file").value(true))
                .andExpect(jsonPath("$.content").value("file://test_image.jpg"));
    }

    @Test
    void testRateLimit_sendFileMessage() throws Exception {
        SendMessageDTO sendMessageDTO = new SendMessageDTO();
        sendMessageDTO.setChatId(chatAB.getChatId());
        sendMessageDTO.setContent("file://test_image.jpg");
        sendMessageDTO.setFile(true);

        for (int i = 0; i < RATE_LIMIT_VALUE; i++) {
            mockMvc.perform(post("/api/messages/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sendMessageDTO)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendMessageDTO)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void removeMessageReaction_Success() throws Exception {
        // First add a reaction
        UpdateMessageReactDTO updateDTO = new UpdateMessageReactDTO();
        updateDTO.setMessageReact(MessageReact.LIKE);

        mockMvc.perform(put("/api/messages/{messageId}/reaction", messageFromB.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());

        // Then remove it
        mockMvc.perform(delete("/api/messages/{messageId}/reaction", messageFromB.getMessageId()))
                .andExpect(status().isNoContent());

        // Verify reaction was removed
        Optional<MessageReaction> reaction = messageReactionRepository.findByMessage_MessageIdAndUser_UserId(
                messageFromB.getMessageId(), userA.getUserId());
        assertFalse(reaction.isPresent());
    }

    @Test
    void getAllReactionsForMessage_Success() throws Exception {
        // Add reactions from both users
        UpdateMessageReactDTO updateDTO = new UpdateMessageReactDTO();
        updateDTO.setMessageReact(MessageReact.LIKE);

        mockMvc.perform(put("/api/messages/{messageId}/reaction", messageFromB.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());

        // Add reaction from userB
        tearDown();
        setupSecurityContext(userB);

        updateDTO.setMessageReact(MessageReact.LOVE);
        mockMvc.perform(put("/api/messages/{messageId}/reaction", messageFromA.getMessageId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());

        // Check all reactions for messageB
        tearDown();
        setupSecurityContext(userA);

        mockMvc.perform(get("/api/messages/{messageId}/reactions", messageFromB.getMessageId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$[0].reactionType").value("LIKE"));
    }

    @Test
    void deleteMessage_Success() throws Exception {
        mockMvc.perform(delete("/api/messages/{messageId}", messageFromA.getMessageId()))
                .andExpect(status().isNoContent());

        // Verify message was deleted or marked as deleted
        Optional<Message> deletedMessage = messageRepository.findById(messageFromA.getMessageId());
        assertTrue(deletedMessage.isEmpty());
    }

    @Test
    void deleteMessage_AnotherUserMessage_ShouldFail() throws Exception {
        mockMvc.perform(delete("/api/messages/{messageId}", messageFromB.getMessageId()))
                .andExpect(status().isForbidden());
    }

}