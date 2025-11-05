package com.example.adoption_and_breeding_module.service;

import com.example.adoption_and_breeding_module.exception.UserNotFound;
import com.example.adoption_and_breeding_module.model.entity.Follow;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.event.FollowEvent;
import com.example.adoption_and_breeding_module.repository.FollowRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.service.impl.FollowListener;
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

class FollowListenerTest {

    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;

    @InjectMocks FollowListener listener;

    UUID followId = UUID.randomUUID();
    UUID followerId = UUID.randomUUID();
    UUID followedId = UUID.randomUUID();

    FollowEvent event;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        event = FollowEvent.builder()
                .followId(followId)
                .followerId(followerId)
                .followedId(followedId)
                .createdAt(Timestamp.from(Instant.now()))
                .build();
    }

    // -------- onFollowAdded --------

    @Test
    void savesFollow_WhenNotExists() {
        when(followRepository.existsById(followId)).thenReturn(false);
        when(userRepository.findById(followerId)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(followedId)).thenReturn(Optional.of(new User()));

        listener.onFollowAdded(event);

        verify(followRepository).save(any(Follow.class));
    }

    @Test
    void doesNotSave_WhenAlreadyExists() {
        when(followRepository.existsById(followId)).thenReturn(true);

        listener.onFollowAdded(event);

        verify(followRepository, never()).save(any());
    }

    @Test
    void throws_WhenFollowerNotFound() {
        when(followRepository.existsById(followId)).thenReturn(false);
        when(userRepository.findById(followerId)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> listener.onFollowAdded(event));
    }

    @Test
    void throws_WhenFollowedNotFound() {
        when(followRepository.existsById(followId)).thenReturn(false);
        when(userRepository.findById(followerId)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(followedId)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> listener.onFollowAdded(event));
    }

    // -------- onFollowRemoved --------

    @Test
    void deletesFollow_WhenExists() {
        when(followRepository.existsById(followId)).thenReturn(true);

        listener.onFollowRemoved(event);

        verify(followRepository).deleteById(followId);
    }

    @Test
    void doesNotDelete_WhenNotExists() {
        when(followRepository.existsById(followId)).thenReturn(false);

        listener.onFollowRemoved(event);

        verify(followRepository, never()).deleteById(any());
    }
}
