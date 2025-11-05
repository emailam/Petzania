package com.example.registrationmodule.exception.rateLimiting;

public class RateLimitExceeded extends RuntimeException {
    public RateLimitExceeded(String message) {
        super(message);
    }
}
