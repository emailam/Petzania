package com.example.registrationmodule.exception.rateLimiting;

// 429 Too Many Requests
public class TooManyRefreshRequests extends RuntimeException {
    public TooManyRefreshRequests(String message) {
        super(message);
    }
}
