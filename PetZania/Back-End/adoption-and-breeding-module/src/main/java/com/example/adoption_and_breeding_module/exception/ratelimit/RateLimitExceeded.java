package com.example.adoption_and_breeding_module.exception.ratelimit;


// 429 Too Many Requests
public class RateLimitExceeded extends RuntimeException {
    public RateLimitExceeded(String message) {
        super(message);
    }
}

