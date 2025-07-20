package com.example.friends.and.chats.module.service;

import com.example.friends.and.chats.module.exception.user.*;
import com.example.friends.and.chats.module.model.dto.friend.FriendDTO;
import com.example.friends.and.chats.module.model.dto.friend.FriendRequestDTO;
import com.example.friends.and.chats.module.model.entity.*;
import com.example.friends.and.chats.module.repository.*;
import com.example.friends.and.chats.module.service.impl.FriendService;
import com.example.friends.and.chats.module.service.IDTOConversionService;
import com.example.friends.and.chats.module.service.impl.BlockPublisher;
import com.example.friends.and.chats.module.service.impl.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FriendServiceTest {

    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserRepository userRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private FollowRepository followRepository;
    @Mock private IDTOConversionService dtoConversionService;
    @Mock private BlockPublisher blockPublisher;
    @Mock private NotificationPublisher notificationPublisher;

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
        User sender = new User(); sender.setUserId(senderId);
        User receiver = new User(); receiver.setUserId(receiverId);

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
        verify(friendRequestRepository, times(1)).save(any());
        verify(notificationPublisher, times(1)).sendFriendRequestNotification(eq(senderId), eq(receiverId), any());
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
        User sender = new User(); sender.setUserId(senderId);
        User receiver = new User(); receiver.setUserId(receiverId);

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.existsByUser1AndUser2(any(), any())).thenReturn(true);

        assertThrows(FriendshipAlreadyExist.class, () -> friendService.sendFriendRequest(senderId, receiverId));
    }

    @Test
    void sendFriendRequest_blocked_throws() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        User sender = new User(); sender.setUserId(senderId);
        User receiver = new User(); receiver.setUserId(receiverId);

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
        User sender = new User(); sender.setUserId(senderId);
        User receiver = new User(); receiver.setUserId(receiverId);
        FriendRequest request = FriendRequest.builder().id(requestId).sender(sender).receiver(receiver).build();
        Friendship friendship = Friendship.builder().user1(sender).user2(receiver).build();
        FriendDTO friendDTO = new FriendDTO();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendshipRepository.save(any())).thenReturn(friendship);
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        doNothing().when(friendRequestRepository).deleteById(requestId);
        doNothing().when(notificationPublisher).sendFriendRequestAcceptedNotification(senderId, receiverId);
        when(dtoConversionService.mapToFriendDTO(any(), any())).thenReturn(friendDTO);

        FriendDTO result = friendService.acceptFriendRequest(requestId, receiverId);
        assertNotNull(result);
        verify(friendRequestRepository, times(1)).deleteById(requestId);
        verify(notificationPublisher, times(1)).sendFriendRequestAcceptedNotification(senderId, receiverId);
    }

    @Test
    void acceptFriendRequest_wrongReceiver_throws() {
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        User sender = new User(); sender.setUserId(senderId);
        User receiver = new User(); receiver.setUserId(UUID.randomUUID()); // Not the same as receiverId
        FriendRequest request = FriendRequest.builder().id(requestId).sender(sender).receiver(receiver).build();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ForbiddenOperation.class, () -> friendService.acceptFriendRequest(requestId, receiverId));
    }

    @Test
    void cancelFriendRequest_success() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User sender = new User(); sender.setUserId(userId);
        User receiver = new User(); receiver.setUserId(UUID.randomUUID());
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
        User sender = new User(); sender.setUserId(UUID.randomUUID());
        User receiver = new User(); receiver.setUserId(UUID.randomUUID());
        FriendRequest request = FriendRequest.builder().id(requestId).sender(sender).receiver(receiver).build();

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ForbiddenOperation.class, () -> friendService.cancelFriendRequest(requestId, userId));
    }
} 