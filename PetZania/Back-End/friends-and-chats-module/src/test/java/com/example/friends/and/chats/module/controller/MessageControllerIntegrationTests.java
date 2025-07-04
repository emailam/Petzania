package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.TestDataUtil;
import com.example.friends.and.chats.module.model.dto.message.SendMessageDTO;
import com.example.friends.and.chats.module.model.dto.message.UpdateMessageContentDTO;
import com.example.friends.and.chats.module.model.dto.message.UpdateMessageReactDTO;
import com.example.friends.and.chats.module.model.dto.message.UpdateMessageStatusDTO;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private UserRepository userRepository;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User userA;
    private User userB;
    private User userC;
    private Chat chatAB;
    private UserChat userAChatAB;
    private UserChat userBChatAB;
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

        // Verify message was saved
        assertEquals(4, messageRepository.count());

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