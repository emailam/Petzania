package com.example.notificationmodule.service;

import com.example.notificationmodule.model.entity.User;
import com.example.notificationmodule.model.event.UserEvent;
import com.example.notificationmodule.repository.NotificationRepository;
import com.example.notificationmodule.repository.UserRepository;
import com.example.notificationmodule.service.impl.UserEventListener;
import com.example.notificationmodule.util.QueueUtils;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserEventListenerTest {
    final static int maxRetries = 3;

    @Mock
    UserRepository userRepository;
    @Mock
    NotificationRepository notificationRepository;
    @Mock
    QueueUtils queueUtils;
    @Mock
    Channel channel;
    @Mock
    Message message;

    @InjectMocks
    UserEventListener listener;

    UserEvent event;
    UUID userId = UUID.randomUUID();
    String username = "user123";
    String email = "email@example.com";
    private MessageProperties messageProperties;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        messageProperties = mock(MessageProperties.class);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(5L);

        event = UserEvent.builder().userId(userId).username(username).email(email).build();
    }

    @Test
    void registersNewUser() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);

        listener.onUserRegistered(event, channel, message);

        verify(userRepository).save(any(User.class));
        verify(channel).basicAck(5L, false);
    }

    @Test
    void doesNotRegisterExistingUser() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(true);

        listener.onUserRegistered(event, channel, message);
        verify(userRepository, never()).save(any());
        verify(channel).basicAck(5L, false);
    }

    @Test
    void handlesUserRegisteredExceptionAndRetries() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        doThrow(new RuntimeException("fail")).when(userRepository).save(any(User.class));
        when(queueUtils.getRetryCount(eq(message), anyString())).thenReturn(2);
        doNothing().when(channel).basicNack(5L, false, false);

        listener.onUserRegistered(event, channel, message);

        verify(channel).basicNack(5L, false, false);
    }

    @Test
    void handlesUserRegisteredMaxRetries() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(false);
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        doThrow(new RuntimeException("fail")).when(userRepository).save(any(User.class));
        when(queueUtils.getRetryCount(eq(message), anyString())).thenReturn(maxRetries);
        doNothing().when(channel).basicAck(5L, false);

        listener.onUserRegistered(event, channel, message);

        verify(channel).basicAck(5L, false);
    }

    @Test
    void deletesUser_WhenUserExists() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(true);

        listener.onUserDeleted(event, channel, message);

        verify(notificationRepository).deleteByRecipientId(userId);
        verify(notificationRepository).deleteByInitiatorId(userId);
        verify(userRepository).deleteById(userId);
        verify(channel).basicAck(5L, false);
    }

    @Test
    void deletesUser_UserNotExists() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(false);

        listener.onUserDeleted(event, channel, message);

        verify(notificationRepository, never()).deleteByRecipientId(userId);
        verify(notificationRepository, never()).deleteByInitiatorId(userId);
        verify(userRepository, never()).deleteById(userId);
        verify(channel).basicAck(5L, false);
    }

    @Test
    void handlesUserDeletedExceptionAndRetries() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(true);
        doThrow(new RuntimeException("fail")).when(notificationRepository).deleteByRecipientId(userId);
        when(queueUtils.getRetryCount(eq(message), anyString())).thenReturn(1);
        doNothing().when(channel).basicNack(5L, false, false);

        listener.onUserDeleted(event, channel, message);

        verify(channel).basicNack(5L, false, false);
    }

    @Test
    void handlesUserDeletedMaxRetries() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(true);
        doThrow(new RuntimeException("fail")).when(notificationRepository).deleteByRecipientId(any());
        when(queueUtils.getRetryCount(eq(message), anyString())).thenReturn(maxRetries);
        doNothing().when(channel).basicAck(5L, false);

        listener.onUserDeleted(event, channel, message);

        verify(channel).basicAck(5L, false);
    }
}