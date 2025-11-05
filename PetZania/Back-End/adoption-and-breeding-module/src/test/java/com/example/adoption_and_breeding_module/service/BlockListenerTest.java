package com.example.adoption_and_breeding_module.service;

import com.example.adoption_and_breeding_module.model.entity.Block;
import com.example.adoption_and_breeding_module.model.entity.User;
import com.example.adoption_and_breeding_module.model.event.BlockEvent;
import com.example.adoption_and_breeding_module.repository.BlockRepository;
import com.example.adoption_and_breeding_module.repository.UserRepository;
import com.example.adoption_and_breeding_module.service.impl.BlockListener;
import com.example.adoption_and_breeding_module.util.QueueUtils;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BlockListenerTest {
    final static int maxRetries = 3;

    @Mock UserRepository userRepository;
    @Mock BlockRepository blockRepository;
    @Mock QueueUtils queueUtils;
    @Mock Channel channel;
    @Mock Message message;

    @InjectMocks BlockListener listener;

    private MessageProperties messageProperties;
    private BlockEvent event;
    private UUID blockId = UUID.randomUUID();
    private UUID blockerId = UUID.randomUUID();
    private UUID blockedId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        messageProperties = mock(MessageProperties.class);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(5L);

        event = BlockEvent.builder()
                .blockId(blockId)
                .blockerId(blockerId)
                .blockedId(blockedId)
                .createdAt(Timestamp.from(Instant.now()))
                .build();
    }

    // ---------- onUserBlocked ----------

    @Test
    void savesBlock_WhenNotExists() throws Exception {
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(false);
        when(userRepository.findById(blockerId)).thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(mock(User.class)));

        listener.onUserBlocked(event, channel, message);

        verify(blockRepository).save(any(Block.class));
        verify(channel).basicAck(5L, false);
    }

    @Test
    void doesNotSave_WhenAlreadyBlocked() throws Exception {
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(true);

        listener.onUserBlocked(event, channel, message);

        verify(blockRepository, never()).save(any());
        verify(channel).basicAck(5L, false);
    }

    @Test
    void retries_OnBlockSaveException() throws Exception {
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(false);
        when(userRepository.findById(blockerId)).thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(mock(User.class)));
        doThrow(new RuntimeException("fail")).when(blockRepository).save(any(Block.class));
        when(queueUtils.getRetryCount(eq(message), anyString())).thenReturn(2);

        listener.onUserBlocked(event, channel, message);

        verify(channel).basicNack(5L, false, false);
    }

    @Test
    void drops_OnBlockSaveMaxRetries() throws Exception {
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(false);
        when(userRepository.findById(blockerId)).thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(mock(User.class)));
        doThrow(new RuntimeException("fail")).when(blockRepository).save(any(Block.class));
        when(queueUtils.getRetryCount(eq(message), anyString())).thenReturn(maxRetries);

        listener.onUserBlocked(event, channel, message);

        verify(channel).basicAck(5L, false);
    }

    // ---------- onUserUnBlocked ----------

    @Test
    void deletesBlock_WhenExists() throws Exception {
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(true);

        listener.onUserUnBlocked(event, channel, message);

        verify(blockRepository).deleteByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId);
        verify(channel).basicAck(5L, false);
    }

    @Test
    void doesNotDelete_WhenNotExists() throws Exception {
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(false);

        listener.onUserUnBlocked(event, channel, message);

        verify(blockRepository, never()).deleteByBlocker_UserIdAndBlocked_UserId(any(), any());
        verify(channel).basicAck(5L, false);
    }

    @Test
    void retries_OnUnblockDeleteException() throws Exception {
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(true);
        doThrow(new RuntimeException("fail")).when(blockRepository)
                .deleteByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId);
        when(queueUtils.getRetryCount(eq(message), anyString())).thenReturn(1);

        listener.onUserUnBlocked(event, channel, message);

        verify(channel).basicNack(5L, false, false);
    }

    @Test
    void drops_OnUnblockMaxRetries() throws Exception {
        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(true);
        doThrow(new RuntimeException("fail")).when(blockRepository)
                .deleteByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId);
        when(queueUtils.getRetryCount(eq(message), anyString())).thenReturn(maxRetries);

        listener.onUserUnBlocked(event, channel, message);

        verify(channel).basicAck(5L, false);
    }
}
