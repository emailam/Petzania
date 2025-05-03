package com.example.registrationmodule.exception.rateLimiting;

// 429 Too Many Requests
public class TooManyPostRequests extends RuntimeException {
    public TooManyPostRequests(String message) {
        super(message);
    }
}

