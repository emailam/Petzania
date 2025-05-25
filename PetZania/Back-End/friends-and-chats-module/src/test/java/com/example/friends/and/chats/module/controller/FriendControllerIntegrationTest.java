package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.TestDataUtil;
import com.example.friends.and.chats.module.model.entity.User;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class FriendControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private FriendRequestRepository friendRequestRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private BlockRepository blockRepository;
    @Autowired private FollowRepository followRepository;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setup() {
        // Clean up first
        friendRequestRepository.deleteAll();
        friendshipRepository.deleteAll();
        blockRepository.deleteAll();
        followRepository.deleteAll();
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
    }

    @Test
    void sendFriendRequest_Success() throws Exception {
        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userB.getUserId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sender.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.receiver.userId").value(userB.getUserId().toString()));
    }

    @Test
    void sendFriendRequest_ToSelfShouldFail() throws Exception {
        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userA.getUserId()))
                .andExpect(status().isForbidden());
    }



}