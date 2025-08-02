package com.example.friends.and.chats.module.exception.ratelimit;

// 429 Too Many Requests
public class RateLimitExceeded extends RuntimeException {
    public RateLimitExceeded(String message) {
        super(message);
    }
}
