package com.example.registrationmodule.service;


import com.example.registrationmodule.exception.user.UserNotFound;
import com.example.registrationmodule.model.entity.Block;
import com.example.registrationmodule.model.entity.User;
import com.example.registrationmodule.model.event.BlockEvent;
import com.example.registrationmodule.repository.BlockRepository;
import com.example.registrationmodule.repository.UserRepository;
import com.example.registrationmodule.service.impl.BlockListener;
import com.example.registrationmodule.util.QueueUtils;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BlockListenerTest {

    private UserRepository userRepository;
    private BlockRepository blockRepository;
    private QueueUtils queueUtils;
    private BlockListener blockListener;
    private Channel channel;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        blockRepository = mock(BlockRepository.class);
        queueUtils = mock(QueueUtils.class);
        channel = mock(Channel.class); // Will throw in some JVMs (see previous explanation)
        blockListener = new BlockListener(userRepository, blockRepository, queueUtils);
    }

    private Message buildMessage(long tag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(tag);
        return new Message("a".getBytes(), props);
    }

    @Test
    void onUserBlocked_savesBlockIfNotExists() throws Exception {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        UUID blockId = UUID.randomUUID();
        BlockEvent event = new BlockEvent(blockId, blockerId, blockedId, null);

        User blocker = new User(); blocker.setUserId(blockerId);
        User blocked = new User(); blocked.setUserId(blockedId);

        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(false);
        when(userRepository.findById(blockerId)).thenReturn(Optional.of(blocker));
        when(userRepository.findById(blockedId)).thenReturn(Optional.of(blocked));
        Message message = buildMessage(100L);

        blockListener.onUserBlocked(event, channel, message);

        verify(blockRepository).save(any(Block.class));
        verify(channel).basicAck(100L, false);
    }

    @Test
    void onUserBlocked_ackIfBlockAlreadyExists() throws Exception {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        UUID blockId = UUID.randomUUID();
        BlockEvent event = new BlockEvent(blockId, blockerId, blockedId, null);

        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(true);
        Message message = buildMessage(21L);

        blockListener.onUserBlocked(event, channel, message);

        verify(blockRepository, never()).save(any());
        verify(channel).basicAck(21L, false);
    }

    @Test
    void onUserBlocked_nacksOnExceptionAndNotAtMaxRetries() throws Exception {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        BlockEvent event = new BlockEvent(UUID.randomUUID(), blockerId, blockedId, null);

        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(false);
        when(userRepository.findById(blockerId)).thenThrow(new UserNotFound("not found"));
        when(queueUtils.getRetryCount(any(), anyString())).thenReturn(1);
        Message message = buildMessage(88L);

        blockListener.onUserBlocked(event, channel, message);

        verify(channel).basicNack(88L, false, false);
    }

    @Test
    void onUserBlocked_acksOnMaxRetries() throws Exception {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        BlockEvent event = new BlockEvent(UUID.randomUUID(), blockerId, blockedId, null);

        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(false);
        when(userRepository.findById(blockerId)).thenThrow(new UserNotFound("not found"));
        when(queueUtils.getRetryCount(any(), anyString())).thenReturn(10); // suppose MAX_RETRIES
        Message message = buildMessage(99L);

        blockListener.onUserBlocked(event, channel, message);

        verify(channel).basicAck(99L, false);
    }

    @Test
    void onUserUnBlocked_deletesIfExists() throws Exception {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        BlockEvent event = new BlockEvent(null, blockerId, blockedId, null);

        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(true);
        Message message = buildMessage(33L);

        blockListener.onUserUnBlocked(event, channel, message);

        verify(blockRepository).deleteByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId);
        verify(channel).basicAck(33L, false);
    }

    @Test
    void onUserUnBlocked_ackIfNotBlocked() throws Exception {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        BlockEvent event = new BlockEvent(null, blockerId, blockedId, null);

        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenReturn(false);
        Message message = buildMessage(37L);

        blockListener.onUserUnBlocked(event, channel, message);

        verify(blockRepository, never()).deleteByBlocker_UserIdAndBlocked_UserId(any(), any());
        verify(channel).basicAck(37L, false);
    }

    @Test
    void onUserUnBlocked_nacksOnExceptionAndNotAtMaxRetries() throws Exception {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        BlockEvent event = new BlockEvent(null, blockerId, blockedId, null);

        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenThrow(new RuntimeException("fail"));
        when(queueUtils.getRetryCount(any(), anyString())).thenReturn(2);
        Message message = buildMessage(44L);

        blockListener.onUserUnBlocked(event, channel, message);

        verify(channel).basicNack(44L, false, false);
    }

    @Test
    void onUserUnBlocked_acksOnMaxRetries() throws Exception {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        BlockEvent event = new BlockEvent(null, blockerId, blockedId, null);

        when(blockRepository.existsByBlocker_UserIdAndBlocked_UserId(blockerId, blockedId)).thenThrow(new RuntimeException("fail"));
        when(queueUtils.getRetryCount(any(), anyString())).thenReturn(10);
        Message message = buildMessage(45L);

        blockListener.onUserUnBlocked(event, channel, message);

        verify(channel).basicAck(45L, false);
    }
}