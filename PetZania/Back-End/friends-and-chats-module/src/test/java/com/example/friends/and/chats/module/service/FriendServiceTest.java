package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.exception.user.*;
import com.example.friends.and.chats.module.model.dto.friend.FriendDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendRequestDTO;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.impl.*;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

class FriendServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private IDTOConversionService dtoConversionService;
    @Mock
    private BlockPublisher blockPublisher;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private FollowProducer followProducer;
    @Mock
    private FriendProducer friendProducer;

    @InjectMocks
    private FriendService friendService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendFriendRequest_success() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        User sender = new User();
        sender.setUserId(senderId);
        User receiver = new User();
        receiver.setUserId(receiverId);

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(friendRequestRepository.existsBySenderAndReceiver(sender, receiver)).thenReturn(false);
        when(friendRequestRepository.existsBySenderAndReceiver(receiver, sender)).thenReturn(false);
        when(friendshipRepository.existsByUser1AndUser2(any(), any())).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(any(), any())).thenReturn(false);
        when(friendRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(dtoConversionService.mapToFriendRequestDTO(any())).thenReturn(new FriendRequestDTO());

        FriendRequestDTO result = friendService.sendFriendRequest(senderId, receiverId);

        assertNotNull(result);
        verify(friendRequestRepository, times(2)).save(any());

        verify(notificationPublisher, times(1))
                .sendFriendRequestNotification(any(), any(), any(), any());
    }

    @Test
    void sendFriendRequest_selfOperation_throws() {
        UUID userId = UUID.randomUUID();
        assertThrows(InvalidOperation.class, () -> friendService.sendFriendRequest(userId, userId));
    }

    @Test
    void sendFriendRequest_alreadyFriends_throws() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        User sender = new User();
        sender.setUserId(senderId);
        User receiver = new User();
        receiver.setUserId(receiverId);

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.existsByUser1AndUser2(any(), any())).thenReturn(true);

        assertThrows(FriendshipAlreadyExist.class, () -> friendService.sendFriendRequest(senderId, receiverId));
    }

    @Test
    void sendFriendRequest_blocked_throws() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        User sender = new User();
        sender.setUserId(senderId);
        User receiver = new User();
        receiver.setUserId(receiverId);

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.existsByUser1AndUser2(any(), any())).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(any(), any())).thenReturn(true);

        assertThrows(BlockingExist.class, () -> friendService.sendFriendRequest(senderId, receiverId));
    }

    @Test
    void acceptFriendRequest_success() {
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        User sender = new User();
        sender.setUserId(senderId);
        User receiver = new User();
        receiver.setUserId(receiverId);
        FriendRequest request = FriendRequest.builder().id(requestId).sender(sender).receiver(receiver).build();
        Friendship friendship = Friendship.builder().user1(sender).user2(receiver).build();
        FriendDTO friendDTO = new FriendDTO();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendshipRepository.save(any())).thenReturn(friendship);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        doNothing().when(friendRequestRepository).deleteById(requestId);
        doNothing().when(notificationPublisher).sendFriendRequestAcceptedNotification(senderId, receiverId, friendship.getId(), receiver.getUsername());
        when(dtoConversionService.mapToFriendDTO(any(), any())).thenReturn(friendDTO);

        FriendDTO result = friendService.acceptFriendRequest(requestId, receiverId);
        assertNotNull(result);
        verify(friendRequestRepository, times(1)).deleteById(requestId);
        verify(notificationPublisher, times(1)).sendFriendRequestAcceptedNotification(senderId, receiverId, friendship.getId(), receiver.getUsername());
    }

    @Test
    void acceptFriendRequest_wrongReceiver_throws() {
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        User sender = new User();
        sender.setUserId(senderId);
        User receiver = new User();
        receiver.setUserId(UUID.randomUUID()); // Not the same as receiverId
        FriendRequest request = FriendRequest.builder().id(requestId).sender(sender).receiver(receiver).build();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ForbiddenOperation.class, () -> friendService.acceptFriendRequest(requestId, receiverId));
    }

    @Test
    void cancelFriendRequest_success() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User sender = new User();
        sender.setUserId(userId);
        User receiver = new User();
        receiver.setUserId(UUID.randomUUID());
        FriendRequest request = FriendRequest.builder().id(requestId).sender(sender).receiver(receiver).build();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        doNothing().when(friendRequestRepository).delete(request);

        assertDoesNotThrow(() -> friendService.cancelFriendRequest(requestId, userId));
        verify(friendRequestRepository, times(1)).delete(request);
    }

    @Test
    void cancelFriendRequest_forbidden_throws() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User sender = new User();
        sender.setUserId(UUID.randomUUID());
        User receiver = new User();
        receiver.setUserId(UUID.randomUUID());
        FriendRequest request = FriendRequest.builder().id(requestId).sender(sender).receiver(receiver).build();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ForbiddenOperation.class, () -> friendService.cancelFriendRequest(requestId, userId));
    }

    @Test
    void followUser_success() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        User follower = new User();
        follower.setUserId(followerId);
        User followed = new User();
        followed.setUserId(followedId);
        Follow follow = Follow.builder().follower(follower).followed(followed).build();

        when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
        when(userRepository.findById(followedId)).thenReturn(Optional.of(followed));
        when(blockRepository.existsByBlockerAndBlocked(any(), any())).thenReturn(false);
        when(followRepository.existsByFollowerAndFollowed(follower, followed)).thenReturn(false);
        when(followRepository.save(any())).thenReturn(follow);
        when(dtoConversionService.mapToFollowDTO(any())).thenReturn(new com.example.friends.and.chats.module.model.dto.friend.FollowDTO());

        com.example.friends.and.chats.module.model.dto.friend.FollowDTO result = friendService.followUser(followerId, followedId);
        assertNotNull(result);
        verify(followRepository, times(1)).save(any());
        verify(notificationPublisher, times(1)).sendNewFollowerNotification(followerId, followedId, follow.getId(), follower.getUsername());
    }

    @Test
    void followUser_selfFollow_throws() {
        UUID userId = UUID.randomUUID();
        assertThrows(InvalidOperation.class, () -> friendService.followUser(userId, userId));
    }

    @Test
    void followUser_alreadyFollowing_throws() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        User follower = new User();
        follower.setUserId(followerId);
        User followed = new User();
        followed.setUserId(followedId);

        when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
        when(userRepository.findById(followedId)).thenReturn(Optional.of(followed));
        when(blockRepository.existsByBlockerAndBlocked(any(), any())).thenReturn(false);
        when(followRepository.existsByFollowerAndFollowed(follower, followed)).thenReturn(true);

        assertThrows(FollowingAlreadyExists.class, () -> friendService.followUser(followerId, followedId));
    }

    @Test
    void followUser_blocked_throws() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        User follower = new User();
        follower.setUserId(followerId);
        User followed = new User();
        followed.setUserId(followedId);

        when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
        when(userRepository.findById(followedId)).thenReturn(Optional.of(followed));
        when(blockRepository.existsByBlockerAndBlocked(any(), any())).thenReturn(true);

        assertThrows(BlockingExist.class, () -> friendService.followUser(followerId, followedId));
    }

    @Test
    void unfollowUser_success() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        User follower = new User();
        follower.setUserId(followerId);
        User followed = new User();
        followed.setUserId(followedId);
        Follow follow = Follow.builder().follower(follower).followed(followed).build();

        when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
        when(userRepository.findById(followedId)).thenReturn(Optional.of(followed));
        when(followRepository.findByFollowerAndFollowed(follower, followed)).thenReturn(Optional.of(follow));
        doNothing().when(followRepository).delete(follow);

        assertDoesNotThrow(() -> friendService.unfollowUser(followerId, followedId));
        verify(followRepository, times(1)).delete(follow);
    }

    @Test
    void unfollowUser_notFollowing_throws() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        User follower = new User();
        follower.setUserId(followerId);
        User followed = new User();
        followed.setUserId(followedId);

        when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
        when(userRepository.findById(followedId)).thenReturn(Optional.of(followed));
        when(followRepository.findByFollowerAndFollowed(follower, followed)).thenReturn(Optional.empty());

        assertThrows(FollowingDoesNotExist.class, () -> friendService.unfollowUser(followerId, followedId));
    }

    @Test
    void blockUser_success() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        User blocker = new User();
        blocker.setUserId(blockerId);
        User blocked = new User();
        blocked.setUserId(blockedId);
        Block block = Block.builder().blocker(blocker).blocked(blocked).build();

        when(userRepository.findById(blockerId)).thenReturn(Optional.of(blocker));
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(blocked));
        when(blockRepository.existsByBlockerAndBlocked(blocker, blocked)).thenReturn(false);
        when(blockRepository.save(any())).thenReturn(block);
        when(dtoConversionService.mapToBlockDTO(any())).thenReturn(new com.example.friends.and.chats.module.model.dto.friend.BlockDTO());

        com.example.friends.and.chats.module.model.dto.friend.BlockDTO result = friendService.blockUser(blockerId, blockedId);
        assertNotNull(result);
        verify(blockRepository, times(1)).save(any());
    }

    @Test
    void blockUser_alreadyBlocked_throws() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        User blocker = new User();
        blocker.setUserId(blockerId);
        User blocked = new User();
        blocked.setUserId(blockedId);

        when(userRepository.findById(blockerId)).thenReturn(Optional.of(blocker));
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(blocked));
        when(blockRepository.existsByBlockerAndBlocked(blocker, blocked)).thenReturn(true);

        assertThrows(BlockingAlreadyExist.class, () -> friendService.blockUser(blockerId, blockedId));
    }

    @Test
    void unblockUser_success() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        User blocker = new User();
        blocker.setUserId(blockerId);
        User blocked = new User();
        blocked.setUserId(blockedId);
        Block block = Block.builder().blocker(blocker).blocked(blocked).build();

        when(userRepository.findById(blockerId)).thenReturn(Optional.of(blocker));
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(blocked));
        when(blockRepository.findByBlockerAndBlocked(blocker, blocked)).thenReturn(Optional.of(block));
        doNothing().when(blockRepository).delete(block);

        assertDoesNotThrow(() -> friendService.unblockUser(blockerId, blockedId));
        verify(blockRepository, times(1)).delete(block);
    }

    @Test
    void unblockUser_notBlocked_throws() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        User blocker = new User();
        blocker.setUserId(blockerId);
        User blocked = new User();
        blocked.setUserId(blockedId);

        when(userRepository.findById(blockerId)).thenReturn(Optional.of(blocker));
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(blocked));
        when(blockRepository.findByBlockerAndBlocked(blocker, blocked)).thenReturn(Optional.empty());

        assertThrows(BlockingDoesNotExist.class, () -> friendService.unblockUser(blockerId, blockedId));
    }

    @Test
    void getFriendships_returnsCorrectData() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        Friendship friendship = Friendship.builder().user1(user).user2(new User()).build();
        org.springframework.data.domain.Page<Friendship> page = new org.springframework.data.domain.PageImpl<>(List.of(friendship));
        when(friendshipRepository.findFriendsByUserId(eq(userId), any())).thenReturn(page);
        when(dtoConversionService.mapToFriendDTO(any(), any())).thenReturn(new FriendDTO());

        org.springframework.data.domain.Page<FriendDTO> result = friendService.getFriendships(userId, 0, 10, "createdAt", "asc");
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getBlockedUsers_returnsCorrectData() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        Block block = Block.builder().blocker(user).blocked(new User()).build();
        org.springframework.data.domain.Page<Block> page = new org.springframework.data.domain.PageImpl<>(List.of(block));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(blockRepository.findBlocksByBlocker(eq(user), any())).thenReturn(page);
        when(dtoConversionService.mapToBlockDTO(any())).thenReturn(new com.example.friends.and.chats.module.model.dto.friend.BlockDTO());

        org.springframework.data.domain.Page<com.example.friends.and.chats.module.model.dto.friend.BlockDTO> result = friendService.getBlockedUsers(userId, 0, 10, "createdAt", "asc");
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getFollowersCount_returnsCorrectCount() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(followRepository.countByFollowed(user)).thenReturn(5);
        assertEquals(5, friendService.getFollowersCount(userId));
    }

    @Test
    void getBlockedUsersCount_returnsCorrectCount() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(blockRepository.countByBlocker(user)).thenReturn(3);
        assertEquals(3, friendService.getBlockedUsersCount(userId));
    }
    @Test
    void isFriendshipExists_true() {
        User user1 = new User();
        user1.setUserId(UUID.randomUUID());
        User user2 = new User();
        user2.setUserId(UUID.randomUUID());
        when(friendshipRepository.existsByUser1AndUser2(user1, user2)).thenReturn(true);
        when(friendshipRepository.existsByUser1AndUser2(user2, user1)).thenReturn(true);
        assertTrue(friendService.isFriendshipExists(user1, user2));
    }

    @Test
    void isFriendshipExists_false() {
        User user1 = new User();
        user1.setUserId(UUID.randomUUID());
        User user2 = new User();
        user2.setUserId(UUID.randomUUID());
        when(friendshipRepository.existsByUser1AndUser2(user1, user2)).thenReturn(false);
        assertFalse(friendService.isFriendshipExists(user1, user2));
    }

    @Test
    void isFriendshipExistsByUsersId_true() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = new User();
        user1.setUserId(userId1);
        User user2 = new User();
        user2.setUserId(userId2);
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendshipRepository.existsByUser1AndUser2(user1, user2)).thenReturn(true);
        when(friendshipRepository.existsByUser1AndUser2(user2, user1)).thenReturn(true);
        assertTrue(friendService.isFriendshipExistsByUsersId(userId1, userId2));
    }

    @Test
    void isFriendshipExistsByUsersId_false() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = new User();
        user1.setUserId(userId1);
        User user2 = new User();
        user2.setUserId(userId2);
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(friendshipRepository.existsByUser1AndUser2(user1, user2)).thenReturn(false);
        assertFalse(friendService.isFriendshipExistsByUsersId(userId1, userId2));
    }

    @Test
    void getReceivedFriendRequests_returnsCorrectData() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);
        FriendRequest request = FriendRequest.builder().id(UUID.randomUUID()).sender(new User()).receiver(user).build();
        org.springframework.data.domain.Page<FriendRequest> page = new org.springframework.data.domain.PageImpl<>(List.of(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(friendRequestRepository.findFriendRequestsByReceiver(eq(user), any())).thenReturn(page);
        when(dtoConversionService.mapToFriendRequestDTO(any())).thenReturn(new FriendRequestDTO());

        org.springframework.data.domain.Page<FriendRequestDTO> result = friendService.getReceivedFriendRequests(userId, 0, 10, "createdAt", "asc");
        assertEquals(1, result.getTotalElements());
    }
} 