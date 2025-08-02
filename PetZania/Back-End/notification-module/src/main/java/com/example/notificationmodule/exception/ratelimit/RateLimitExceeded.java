package com.example.notificationmodule.exception.ratelimit;

public class RateLimitExceeded extends RuntimeException {
    public RateLimitExceeded(String message) {
        super(message);
    }
}

