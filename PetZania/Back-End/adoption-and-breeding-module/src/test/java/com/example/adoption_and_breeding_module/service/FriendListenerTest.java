package com.example.adoption_and_breeding_module.service;

import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.entity.Friendship;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.event.FriendEvent;
import com.example.adoption_and_breeding_module.repository.FriendshipRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.service.impl.FriendListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FriendListenerTest {

    @Mock UserRepository userRepository;
    @Mock FriendshipRepository friendshipRepository;

    @InjectMocks FriendListener listener;

    UUID friendshipId = UUID.randomUUID();
    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();

    FriendEvent event;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        event = FriendEvent.builder()
                .friendshipId(friendshipId)
                .user1Id(user1Id)
                .user2Id(user2Id)
                .createdAt(Timestamp.from(Instant.now()))
                .build();
    }

    // -------- onFriendAdded --------

    @Test
    void savesFriendship_WhenNotExists() {
        when(friendshipRepository.existsById(friendshipId)).thenReturn(false);
        when(userRepository.findById(user1Id)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(user2Id)).thenReturn(Optional.of(new User()));

        listener.onFriendAdded(event);

        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    void doesNotSave_WhenAlreadyExists() {
        when(friendshipRepository.existsById(friendshipId)).thenReturn(true);

        listener.onFriendAdded(event);

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void throws_WhenUserNotFound() {
        when(friendshipRepository.existsById(friendshipId)).thenReturn(false);
        when(userRepository.findById(user1Id)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> listener.onFriendAdded(event));
    }

    // -------- onFriendRemoved --------

    @Test
    void deletesFriendship_WhenExists() {
        when(friendshipRepository.existsById(friendshipId)).thenReturn(true);

        listener.onFriendRemoved(event);

        verify(friendshipRepository).deleteById(friendshipId);
    }

    @Test
    void doesNotDelete_WhenNotExists() {
        when(friendshipRepository.existsById(friendshipId)).thenReturn(false);

        listener.onFriendRemoved(event);

        verify(friendshipRepository, never()).deleteById(any());
    }
}
