package com.example.friends.and.chats.module.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for tracking processed events to ensure idempotency
 * Uses Redis for distributed applications, but falls back to in-memory storage for simpler setups
 */
@Service
public class EventTrackingService {

    // In-memory fallback storage
    private Set<String> processedEventIds = new HashSet<>();

    // Optional Redis implementation for distributed environments
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    // Redis key prefix for event tracking
    private static final String EVENT_KEY_PREFIX = "processed_event:";

    // How long to keep the event IDs (7 days in seconds)
    private static final long EVENT_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    /**
     * Mark the event as processed with distributed support
     *
     * @param eventId The unique ID of the event
     * @return true if the event was newly marked, false if it was already processed
     */
    public boolean markEventAsProcessed(String eventId) {
        if (redisTemplate != null) {
            // Use Redis for distributed applications
            String key = EVENT_KEY_PREFIX + eventId;
            Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, "1", EVENT_EXPIRY_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(wasAbsent);
        } else {
            // Fallback to in-memory for simpler applications
            if (processedEventIds.contains(eventId)) {
                return false; // Already processed
            } else {
                processedEventIds.add(eventId);
                return true;
            }
        }
    }

    /**
     * Check if an event has already been processed
     *
     * @param eventId The unique ID of the event
     * @return true if the event was previously processed
     */
    public boolean isEventProcessed(String eventId) {
        if (redisTemplate != null) {
            // Use Redis for distributed applications
            String key = EVENT_KEY_PREFIX + eventId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } else {
            // Fallback to in-memory for simpler applications
            return processedEventIds.contains(eventId);
        }
    }

    /**
     * Remove an event from the processed list (useful when processing fails)
     *
     * @param eventId The unique ID of the event to unmark
     */
    public void unmarkEvent(String eventId) {
        if (redisTemplate != null) {
            // Use Redis for distributed applications
            String key = EVENT_KEY_PREFIX + eventId;
            redisTemplate.delete(key);
        } else {
            // Fallback to in-memory for simpler applications
            processedEventIds.remove(eventId);
        }
    }
}