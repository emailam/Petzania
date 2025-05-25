package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.TestDataUtil;
import com.example.friends.and.chats.module.model.entity.Block;
import com.example.friends.and.chats.module.model.entity.Friendship;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class FriendControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FriendRequestRepository friendRequestRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private FollowRepository followRepository;

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

    @Test
    void sendFriendRequest_WhenBlockedShouldFail() throws Exception {
        // UserB blocks UserA
        blockRepository.save(Block.builder()
                .blocker(userB)
                .blocked(userA)
                .build());

        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userB.getUserId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Operation blocked due to existing block relationship"));
    }

    @Test
    void sendDuplicateFriendRequest_ShouldFail() throws Exception {
        // First request succeeds
        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userB.getUserId()))
                .andExpect(status().isCreated());

        // Second request fails
        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userB.getUserId()))
                .andExpect(status().isConflict());
    }

    @Test
    void sendFriendRequest_ToNonExistentUser_ShouldFail() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        mockMvc.perform(post("/api/friends/send-request/{receiverId}", nonExistentUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptFriendRequest_Success() throws Exception {
        // UserB sends request to UserA
        changeSecurityContext(userB);
        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userA.getUserId()))
                .andExpect(status().isCreated());

        // Switch back to UserA and accept
        changeSecurityContext(userA);
        UUID requestId = friendRequestRepository.findAll().get(0).getId();

        if (userA.getUserId().compareTo(userB.getUserId()) > 0) {
            User temp = userA;
            userA = userB;
            userB = userA;
        }
        mockMvc.perform(post("/api/friends/accept-request/{requestId}", requestId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user1.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.user2.userId").value(userB.getUserId().toString()));

    }

    @Test
    void acceptNonExistentRequest_ShouldFail() throws Exception {
        UUID nonExistentRequestId = UUID.randomUUID();
        mockMvc.perform(post("/api/friends/accept-request/{requestId}", nonExistentRequestId))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptRequest_NotReceiver_ShouldFail() throws Exception {
        // UserB sends request to UserA
        changeSecurityContext(userB);
        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userA.getUserId()))
                .andExpect(status().isCreated());

        // UserC tries to accept (not involved in request)
        changeSecurityContext(userC);
        UUID requestId = friendRequestRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/friends/accept-request/{requestId}", requestId))
                .andExpect(status().isForbidden());
    }
    @Test
    void removeFriend_Success() throws Exception {
        // Create friendship first
        friendshipRepository.save(Friendship.builder()
                .user1(userA)
                .user2(userB)
                .build());

        mockMvc.perform(delete("/api/friends/remove/{friendId}", userB.getUserId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string("Friend was removed successfully!"));
    }
    // Helper method to switch security context
    private void changeSecurityContext(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }


}