package com.example.registrationmodule.exception.rateLimiting;

// 429 Too Many Requests
public class TooManyAdminRequests extends RuntimeException {
    public TooManyAdminRequests(String message) {
        super(message);
    }
}
