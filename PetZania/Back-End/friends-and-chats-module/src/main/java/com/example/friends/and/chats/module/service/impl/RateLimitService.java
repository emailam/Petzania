package com.example.friends.and.chats.module.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final RedisTemplate<String, String> redisTemplate;

    public boolean isAllowed(String userId, String methodName, int maxRequests, int durationSeconds) {
        String key = "rate_limit:" + userId + ":" + methodName;
        String currentCount = redisTemplate.opsForValue().get(key);

        if (currentCount == null) {
            // First request in this window
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(durationSeconds));
            return true;
        }

        int count = Integer.parseInt(currentCount);
        if (count >= maxRequests) {
            return false; // Rate limit exceeded
        }

        System.out.println(key);
        // Increment counter
        redisTemplate.opsForValue().increment(key);
        return true;
    }
}
