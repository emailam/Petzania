package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.TestDataUtil;
import com.example.friends.and.chats.module.model.dto.chat.UpdateUserChatDTO;
import com.example.friends.and.chats.module.model.entity.Chat;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.entity.UserChat;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.repository.ChatRepository;
import com.example.friends.and.chats.module.repository.UserChatRepository;
import com.example.friends.and.chats.module.repository.UserRepository;
import com.example.friends.and.chats.module.service.IChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ChatControllerIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private UserChatRepository userChatRepository;
    @Autowired
    private IChatService chatService;

    private User userA;
    private User userB;
    private User userC;
    private Chat chatAB;
    private UserChat userAChatAB;
    private UserChat userBChatAB;

    @BeforeEach
    void setup() {
        // Clean up first
        userChatRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        userA = userRepository.save(TestDataUtil.createTestUser("userA"));
        userB = userRepository.save(TestDataUtil.createTestUser("UserB"));
        userC = userRepository.save(TestDataUtil.createTestUser("UserC"));

        // Set up security context
        UserPrincipal userPrincipal = new UserPrincipal(userA);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                )
        );

        chatAB = chatRepository.save(
                Chat.builder()
                        .user1(userA)
                        .user2(userB)
                        .build());

        userAChatAB = userChatRepository.save(
                UserChat.builder()
                        .chat(chatAB)
                        .user(userA)
                        .pinned(false)
                        .unread(0)
                        .muted(false)
                        .build());

        userBChatAB = userChatRepository.save(
                UserChat.builder()
                        .chat(chatAB)
                        .user(userB)
                        .pinned(false)
                        .unread(0)
                        .muted(false)
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
    void createChatIfNotExists_Success() throws Exception {
        mockMvc.perform(post("/api/chats/user/{user2Id}", userC.getUserId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user1Id").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.user2Id").value(userC.getUserId().toString()));

        // Verify chat was created
        List<Chat> chats = chatRepository.findAll();
        assertEquals("Size should be 2", 2, chats.size()); // Original + new chat

        // Verify UserChat entries were created
        List<UserChat> userChats = userChatRepository.findAll();
        assertEquals("Size should be 4", 4, userChats.size()); // Original 2 + 2 new entries
    }

    @Test
    void createChatIfNotExists_WithExistingChat_ReturnsExistingChat() throws Exception {
        mockMvc.perform(post("/api/chats/user/{user2Id}", userB.getUserId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chatId").value(chatAB.getChatId().toString()));

        // Verify no new chat was created
        assertEquals("Number of chats should remain the same", 1, (int) chatRepository.count());
    }

    @Test
    void createChatIfNotExists_WithSelf_ShouldFail() throws Exception {
        mockMvc.perform(post("/api/chats/user/{user2Id}", userA.getUserId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createChatIfNotExists_WithNonExistentUser_ShouldFail() throws Exception {
        // Mock authentication to return userA's ID

        UUID nonExistentUserId = UUID.randomUUID();
        mockMvc.perform(post("/api/chats/user/{user2Id}", nonExistentUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getChatById_Success() throws Exception {

        mockMvc.perform(get("/api/chats/{chatId}", chatAB.getChatId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatId").value(chatAB.getChatId().toString()))
                .andExpect(jsonPath("$.user1Id").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.user2Id").value(userB.getUserId().toString()));
    }

    @Test
    void getChatById_NonExistentChat_ShouldFail() throws Exception {
        UUID nonExistentChatId = UUID.randomUUID();
        mockMvc.perform(get("/api/chats/{chatId}", nonExistentChatId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserChatByChatId_Success() throws Exception {
        mockMvc.perform(get("/api/chats/{chatId}/user-chat", chatAB.getChatId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userChatId").value(userAChatAB.getUserChatId().toString()))
                .andExpect(jsonPath("$.chatId").value(chatAB.getChatId().toString()))
                .andExpect(jsonPath("$.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.pinned").value(false))
                .andExpect(jsonPath("$.unread").value(0))
                .andExpect(jsonPath("$.muted").value(false));
    }

    @Test
    void getUserChatByChatId_Success_NonExistentUserChat_ShouldFail() throws Exception {
        UUID nonExistentUserChatId = UUID.randomUUID();
        mockMvc.perform(get("/api/chats/{chatId}/user-chat", nonExistentUserChatId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserChats_Success() throws Exception {

        mockMvc.perform(get("/api/chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].chatId").value(chatAB.getChatId().toString()));
    }

    @Test
    void partialUpdateUserChat_Success() throws Exception {
        // Create update request with changes
        UpdateUserChatDTO updateDTO = new UpdateUserChatDTO();
        updateDTO.setPinned(true);
        updateDTO.setMuted(true);

        mockMvc.perform(patch("/api/chats/{chatId}", chatAB.getChatId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(true))
                .andExpect(jsonPath("$.muted").value(true));

        // Verify the changes were persisted
        UserChat updatedUserChat = userChatRepository.findByChat_ChatIdAndUser_UserId(
                chatAB.getChatId(), userA.getUserId()).orElseThrow();
        assertTrue(updatedUserChat.getPinned());
        assertTrue(updatedUserChat.getMuted());
    }

    @Test
    void deleteUserChatById_Success() throws Exception {

        mockMvc.perform(delete("/api/chats/user/{userChatId}", userAChatAB.getUserChatId()))
                .andExpect(status().isNoContent());

        // Verify the UserChat was deleted
        assertFalse(userChatRepository.existsById(userAChatAB.getUserChatId()));
        // Verify the other UserChat still exists
        assertTrue(userChatRepository.existsById(userBChatAB.getUserChatId()));
    }

    @Test
    void deleteUserChatById_NonExistentUserChat_ShouldFail() throws Exception {
        UUID nonExistentUserChatId = UUID.randomUUID();
        mockMvc.perform(delete("/api/chats/user/{userChatId}", nonExistentUserChatId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUserChatById_NotOwner_ShouldFail() throws Exception {
        // Try to delete userB's UserChat
        mockMvc.perform(delete("/api/chats/user/{userChatId}", userBChatAB.getUserChatId()))
                .andExpect(status().isForbidden());

        // Verify the UserChat was not deleted
        assertTrue(userChatRepository.existsById(userBChatAB.getUserChatId()));
    }

}
