package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.TestDataUtil;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.IFriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Transactional
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
    @Autowired
    private IFriendService friendService;

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

        List<FriendRequest> requests = friendRequestRepository.findAll();
        int expectedSize = 1;
        assertEquals(expectedSize, requests.size());
        assertEquals(userA.getUserId(), requests.get(0).getSender().getUserId());
        assertEquals(userB.getUserId(), requests.get(0).getReceiver().getUserId());
    }

    @Test
    void sendFriendRequest_ToSelfShouldFail() throws Exception {
        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userA.getUserId()))
                .andExpect(status().isBadRequest());
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
        // UserB sends a friend request to UserA
        friendService.sendFriendRequest(userB.getUserId(), userA.getUserId());

        UUID requestId = friendRequestRepository.findAll().get(0).getId();

        if (userA.getUserId().compareTo(userB.getUserId()) > 0) {
            User temp = userA;
            userA = userB;
            userB = temp;
        }

        // UserA tries to accept this friend request
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
        friendService.createFriendship(userA, userB);


        mockMvc.perform(delete("/api/friends/remove/{friendId}", userB.getUserId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string("Friend was removed successfully!"));
    }

    @Test
    void removeNonFriend_ShouldFail() throws Exception {
        mockMvc.perform(delete("/api/friends/remove/{friendId}", userB.getUserId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void blockUser_Success() throws Exception {
        mockMvc.perform(post("/api/friends/block/{userId}", userB.getUserId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blocker.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.blocked.userId").value(userB.getUserId().toString()));

        assertTrue(blockRepository.existsByBlockerAndBlocked(userA, userB));
    }

    @Test
    void blockUser_WhenAlreadyBlocked_ShouldFail() throws Exception {
        // First block succeeds
        mockMvc.perform(post("/api/friends/block/{userId}", userB.getUserId()))
                .andExpect(status().isCreated());

        // Second block should fail
        mockMvc.perform(post("/api/friends/block/{userId}", userB.getUserId()))
                .andExpect(status().isConflict());
    }

    @Test
    void blockUser_WhenAlreadyFriends_ShouldCleanup() throws Exception {
        // Create friendship and following
        friendService.createFriendship(userA, userB);
        friendService.followUser(userA.getUserId(), userB.getUserId());
        friendService.followUser(userB.getUserId(), userA.getUserId());
        friendshipRepository.save(Friendship.builder()
                .user1(userA)
                .user2(userB)
                .build());

        User tempA = userA;
        User tempB = userB;
        if (tempA.getUserId().compareTo(tempB.getUserId()) > 0) {
            User temp = tempA;
            tempA = tempB;
            tempB = temp;
        }

        mockMvc.perform(post("/api/friends/block/{userId}", userB.getUserId()))
                .andExpect(status().isCreated());
        assertFalse("", followRepository.existsByFollowerAndFollowed(userA, userB));
        assertFalse("", followRepository.existsByFollowerAndFollowed(userB, userA));
        assertFalse("", friendshipRepository.existsByUser1AndUser2(tempA, tempB));
        assertTrue(blockRepository.existsByBlockerAndBlocked(userA, userB));
    }

    @Test
    void followUser_Success() throws Exception {
        mockMvc.perform(post("/api/friends/follow/{userId}", userB.getUserId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.follower.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.followed.userId").value(userB.getUserId().toString()));

        assertTrue(followRepository.existsByFollowerAndFollowed(userA, userB));
    }

    @Test
    void followUser_WhenBlocked_ShouldFail() throws Exception {
        blockRepository.save(Block.builder()
                .blocker(userB)
                .blocked(userA)
                .build());

        mockMvc.perform(post("/api/friends/follow/{userId}", userB.getUserId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unfollowUser_Success() throws Exception {
        // First follow
        friendService.followUser(userA.getUserId(), userB.getUserId());

        mockMvc.perform(put("/api/friends/unfollow/{userId}", userB.getUserId()))
                .andExpect(status().isNoContent());

        assertFalse("", followRepository.existsByFollowerAndFollowed(userA, userB));
    }

    @Test
    void cancelSentRequest_Success() throws Exception {
        FriendRequest request = friendRequestRepository.save(FriendRequest.builder()
                .sender(userA)
                .receiver(userB)
                .build());

        mockMvc.perform(put("/api/friends/decline-request/{requestId}", request.getId())).andExpect(status().isNoContent());

        assertFalse("", friendRequestRepository.existsById(request.getId()));
    }

    @Test
    void sendRequest_WhenAlreadyFriends_ShouldFail() throws Exception {
        // Create friendship first
        friendService.createFriendship(userA, userB);

        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userB.getUserId()))
                .andExpect(status().isConflict());
    }

    @Test
    void followSelf_ShouldFail() throws Exception {
        mockMvc.perform(post("/api/friends/follow/{userId}", userA.getUserId()))
                .andExpect(status().isBadRequest());
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