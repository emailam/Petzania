package com.example.registrationmodule.exception.rateLimiting;

// 429 Too Many Requests
public class TooManyLogoutRequests extends RuntimeException {
    public TooManyLogoutRequests(String message) {
        super(message);
    }
}
