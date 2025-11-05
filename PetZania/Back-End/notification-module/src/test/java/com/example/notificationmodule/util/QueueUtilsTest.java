package com.example.notificationmodule.util;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueueUtilsTest {

    private final QueueUtils queueUtils = new QueueUtils();

    @Test
    void returnsCountWhenRetryQueueMatchAndCountPresent() {
        MessageProperties props = new MessageProperties();
        String retryQueue = "retry.q";
        long expectedCount = 3L;

        // x-death header value
        Map<String, Object> deathMap = new HashMap<>();
        deathMap.put("queue", retryQueue);
        deathMap.put("count", expectedCount);
        List<Map<String, Object>> deaths = List.of(deathMap);

        props.getHeaders().put("x-death", deaths);
        Message message = new Message(new byte[0], props);

        int retryCount = queueUtils.getRetryCount(message, retryQueue);
        assertEquals((int) expectedCount, retryCount);
    }

    @Test
    void returnsZeroWhenNoXDeathHeader() {
        MessageProperties props = new MessageProperties();
        Message message = new Message(new byte[0], props);

        int retryCount = queueUtils.getRetryCount(message, "retry.q");
        assertEquals(0, retryCount);
    }

    @Test
    void returnsZeroWhenXDeathFormatIncorrect() {
        MessageProperties props = new MessageProperties();
        props.getHeaders().put("x-death", "not-a-list");
        Message message = new Message(new byte[0], props);

        int retryCount = queueUtils.getRetryCount(message, "retry.q");
        assertEquals(0, retryCount);
    }

    @Test
    void returnsZeroWhenNoMatchingQueue() {
        MessageProperties props = new MessageProperties();
        String retryQueue = "retry.q";
        Map<String, Object> deathMap = new HashMap<>();
        deathMap.put("queue", "other-queue");
        deathMap.put("count", 2L);

        List<Map<String, Object>> deaths = List.of(deathMap);
        props.getHeaders().put("x-death", deaths);
        Message message = new Message(new byte[0], props);

        int retryCount = queueUtils.getRetryCount(message, retryQueue);
        assertEquals(0, retryCount);
    }

    @Test
    void returnsZeroWhenCountIsNotNumber() {
        MessageProperties props = new MessageProperties();
        String retryQueue = "retry.q";
        Map<String, Object> deathMap = new HashMap<>();
        deathMap.put("queue", retryQueue);
        deathMap.put("count", "not-a-number");

        List<Map<String, Object>> deaths = List.of(deathMap);
        props.getHeaders().put("x-death", deaths);
        Message message = new Message(new byte[0], props);

        int retryCount = queueUtils.getRetryCount(message, retryQueue);
        assertEquals(0, retryCount);
    }
}