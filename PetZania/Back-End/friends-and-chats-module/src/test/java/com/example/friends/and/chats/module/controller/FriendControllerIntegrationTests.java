package com.example.friends.and.chats.module.controller;

import com.example.friends.and.chats.module.TestDataUtil;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.model.principal.UserPrincipal;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.IFriendService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Transactional
public class FriendControllerIntegrationTests {

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
    private User userD;

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
        userD = userRepository.save(TestDataUtil.createTestUser("UserD"));

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
    @AfterEach
    void tearDown() {
        // Clear the security context after each test
        SecurityContextHolder.clearContext();
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
        setupSecurityContext(userB);
        mockMvc.perform(post("/api/friends/send-request/{receiverId}", userA.getUserId()))
                .andExpect(status().isCreated());

        // UserC tries to accept (not involved in request)
        setupSecurityContext(userC);
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

    @Test
    void getFriendships_Success() throws Exception {
        // Create friendships
        Friendship friendship1 = friendService.createFriendship(userA, userB);
        Friendship friendship2 = friendService.createFriendship(userA, userC);

        mockMvc.perform(get("/api/friends/getFriends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].friendshipId").value(friendship1.getId().toString()))
                .andExpect(jsonPath("$.content[1].friendshipId").value(friendship2.getId().toString()));
    }

    @Test
    void getFriendships_Pagination() throws Exception {
        friendService.createFriendship(userA, userB);
        friendService.createFriendship(userA, userC);
        friendService.createFriendship(userA, userD);

        mockMvc.perform(get("/api/friends/getFriends?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getNumberOfFriends_Success() throws Exception {
        friendService.createFriendship(userA, userB);
        friendService.createFriendship(userC, userA);
        friendService.createFriendship(userA, userC);

        mockMvc.perform(get("/api/friends/getNumberOfFriends"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void getFollowing_ReturnsPaginatedResults() throws Exception {
        createFollows(userA, userB, userC, userD);

        mockMvc.perform(get("/api/friends/getFollowing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].follower.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].followed.userId").value(userB.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].follower.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].followed.userId").value(userC.getUserId().toString()))
                .andExpect(jsonPath("$.content[2].follower.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[2].followed.userId").value(userD.getUserId().toString()))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getFollowed_ReturnsPaginatedResults() throws Exception {
        createFollows(userB, userA);
        createFollows(userC, userA);
        createFollows(userD, userA);

        mockMvc.perform(get("/api/friends/getFollowers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].follower.userId").value(userB.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].followed.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].follower.userId").value(userC.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].followed.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[2].follower.userId").value(userD.getUserId().toString()))
                .andExpect(jsonPath("$.content[2].followed.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getFollowing_WithCustomPagination() throws Exception {
        createFollows(userA, userB, userC, userD);

        mockMvc.perform(get("/api/friends/getFollowing?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getFollowers_WithCustomPagination() throws Exception {
        createFollows(userB, userA);
        createFollows(userC, userA);
        createFollows(userD, userA);

        mockMvc.perform(get("/api/friends/getFollowers?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getBlockedUsers_WithCustomPagination() throws Exception {
        createBlocks(userA, userB, userC, userD);

        mockMvc.perform(get("/api/friends/getBlockedUsers?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getFollowing_WithSorting() throws Exception {
        createFollows(userA, userB, userC);

        // Test sort by createdAt desc
        mockMvc.perform(get("/api/friends/getFollowing?sortBy=createdAt&direction=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].follower.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].followed.userId").value(userC.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].follower.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].followed.userId").value(userB.getUserId().toString()));

        // Test sort by createdAt asc
        mockMvc.perform(get("/api/friends/getFollowing?sortBy=createdAt&direction=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].follower.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].followed.userId").value(userB.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].follower.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].followed.userId").value(userC.getUserId().toString()));
    }

    @Test
    void getFollowers_WithSorting() throws Exception {
        createFollows(userB, userA);
        createFollows(userC, userA);

        // Test sort by createdAt desc
        mockMvc.perform(get("/api/friends/getFollowers?sortBy=createdAt&direction=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].follower.userId").value(userC.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].followed.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].follower.userId").value(userB.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].followed.userId").value(userA.getUserId().toString()));

        // Test sort by createdAt asc
        mockMvc.perform(get("/api/friends/getFollowers?sortBy=createdAt&direction=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].follower.userId").value(userB.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].followed.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].follower.userId").value(userC.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].followed.userId").value(userA.getUserId().toString()));
    }

    @Test
    void getBlockedUsers_WithSorting() throws Exception {
        createBlocks(userA, userB, userC, userD);

        // Test sort by createdAt desc
        mockMvc.perform(get("/api/friends/getBlockedUsers?sortBy=createdAt&direction=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].blocker.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].blocked.userId").value(userD.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].blocker.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].blocked.userId").value(userC.getUserId().toString()))
                .andExpect(jsonPath("$.content[2].blocker.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[2].blocked.userId").value(userB.getUserId().toString()));

        // Test sort by createdAt asc
        mockMvc.perform(get("/api/friends/getBlockedUsers?sortBy=createdAt&direction=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].blocker.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].blocked.userId").value(userB.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].blocker.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].blocked.userId").value(userC.getUserId().toString()))
                .andExpect(jsonPath("$.content[2].blocker.userId").value(userA.getUserId().toString()))
                .andExpect(jsonPath("$.content[2].blocked.userId").value(userD.getUserId().toString()));
    }

    @Test
    void getFollowing_InvalidSortField() throws Exception {
        createFollows(userA, userB);

        mockMvc.perform(get("/api/friends/getFollowing?sortBy=invalidField"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBlocked_InvalidSortField() throws Exception {
        createBlocks(userA, userB, userC, userD);

        mockMvc.perform(get("/api/friends/getBlockedUsers?sortBy=invalidField"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNumberOfFollowing_Success() throws Exception {
        createFollows(userA, userB, userC);

        mockMvc.perform(get("/api/friends/getNumberOfFollowing"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void getNumberOfFollowing_Zero() throws Exception {
        mockMvc.perform(get("/api/friends/getNumberOfFollowing"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void getNumberOfFollowers_Success() throws Exception {
        createFollows(userB, userA);
        createFollows(userC, userA);
        createFollows(userD, userA);

        mockMvc.perform(get("/api/friends/getNumberOfFollowers"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void getNumberOfBlockedUsers_Success() throws Exception {
        friendService.blockUser(userA.getUserId(), userB.getUserId());
        friendService.blockUser(userA.getUserId(), userC.getUserId());

        mockMvc.perform(get("/api/friends/getNumberOfBlockedUsers"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void getNumberOfBlockedUsers_Zero() throws Exception {
        mockMvc.perform(get("/api/friends/getNumberOfBlockedUsers"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void getFriendships_ReturnsAllFriendshipsForUser() throws Exception {
        // Given
        Friendship friendship1 = friendService.createFriendship(userA, userB);
        Friendship friendship2 = friendService.createFriendship(userA, userC);
        Friendship friendship3 = createFriendship(userB, userC);

        mockMvc.perform(get("/api/friends/getFriends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].friendshipId").value(friendship1.getId().toString()))
                .andExpect(jsonPath("$.content[1].friendshipId").value(friendship2.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getFriendships_PaginationWorksCorrectly() throws Exception {
        // Given - 3 friendships
        createFriendship(userA, userB);
        createFriendship(userA, userC);
        createFriendship(userA, userD);

        // When/Then - Page 1
        mockMvc.perform(get("/api/friends/getFriends?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        // Page 2
        mockMvc.perform(get("/api/friends/getFriends?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getFriendships_SortingByCreatedAtDesc() throws Exception {
        Friendship oldFriendship = createFriendship(userA, userB);
        oldFriendship.setCreatedAt(Timestamp.from(Instant.now().minusSeconds(3600)));
        friendshipRepository.save(oldFriendship);

        Friendship newFriendship = createFriendship(userA, userC);

        mockMvc.perform(get("/api/friends/getFriends?sortBy=createdAt&direction=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].friendshipId").value(newFriendship.getId().toString()))
                .andExpect(jsonPath("$.content[1].friendshipId").value(oldFriendship.getId().toString()));
    }

    @Test
    void getFriendships_SortingByCreatedAtAsc() throws Exception {
        // Given - friendships created at different times
        Friendship oldFriendship = createFriendship(userA, userB);
        oldFriendship.setCreatedAt(Timestamp.from(Instant.now().minusSeconds(3600)));
        friendshipRepository.save(oldFriendship);

        Friendship newFriendship = createFriendship(userA, userC);

        // When/Then
        mockMvc.perform(get("/api/friends/getFriends?sortBy=createdAt&direction=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].friendshipId").value(oldFriendship.getId().toString()))
                .andExpect(jsonPath("$.content[1].friendshipId").value(newFriendship.getId().toString()));
    }

    @Test
    void getFriendships_ReturnsEmptyWhenNoFriendships() throws Exception {
        mockMvc.perform(get("/api/friends/getFriends"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getReceivedFriendRequests_ReturnsPendingRequests() throws Exception {
        friendRequestRepository.saveAll(List.of(
                FriendRequest.builder()
                        .sender(userB)
                        .receiver(userA)
                        .build()
        ));

        mockMvc.perform(get("/api/friends/received-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sender.userId").value(userB.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].receiver.userId").value(userA.getUserId().toString()));
    }

    @Test
    void getReceivedFriendRequests_EmptyWhenNoRequests() throws Exception {
        mockMvc.perform(get("/api/friends/received-requests"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getReceivedFriendRequests_SortingByCreatedAt() throws Exception {
        // Create requests at different times
        FriendRequest oldRequest = friendRequestRepository.save(FriendRequest.builder()
                .sender(userB)
                .receiver(userA)
                .createdAt(Timestamp.from(Instant.now().minusSeconds(3600)))
                .build());

        FriendRequest newRequest = friendRequestRepository.save(FriendRequest.builder()
                .sender(userC)
                .receiver(userA)
                .createdAt(Timestamp.from(Instant.now()))
                .build());

        setupSecurityContext(userA);

        // Test ascending order
        mockMvc.perform(get("/api/friends/received-requests?sortBy=createdAt&direction=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value(oldRequest.getId().toString()))
                .andExpect(jsonPath("$.content[1].requestId").value(newRequest.getId().toString()));

        // Test descending order
        mockMvc.perform(get("/api/friends/received-requests?sortBy=createdAt&direction=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value(newRequest.getId().toString()))
                .andExpect(jsonPath("$.content[1].requestId").value(oldRequest.getId().toString()));
    }

    @Test
    void getReceivedFriendRequests_OnlyReturnsPendingRequests() throws Exception {
        friendRequestRepository.saveAll(List.of(
                FriendRequest.builder()
                        .sender(userB)
                        .receiver(userA)
                        .build(),
                FriendRequest.builder()
                        .sender(userC)
                        .receiver(userA)
                        .build(),
                FriendRequest.builder()
                        .sender(userD)
                        .receiver(userA)
                        .build()
        ));

        setupSecurityContext(userA);

        mockMvc.perform(get("/api/friends/received-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].sender.userId").value(userB.getUserId().toString()))
                .andExpect(jsonPath("$.content[1].sender.userId").value(userC.getUserId().toString()))
                .andExpect(jsonPath("$.content[2].sender.userId").value(userD.getUserId().toString()));


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

    private void createFollows(User follower, User... followedUsers) {
        for (User followed : followedUsers) {
            friendService.followUser(follower.getUserId(), followed.getUserId());
        }
    }

    private void createBlocks(User blocker, User... blockedUsers) {
        for (User blocked : blockedUsers) {
            friendService.blockUser(blocker.getUserId(), blocked.getUserId());
        }
    }

    private Friendship createFriendship(User user1, User user2) {
        return friendshipRepository.save(Friendship.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(Timestamp.from(Instant.now()))
                .build());
    }


}