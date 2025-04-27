package com.example.registrationmodule.exception.rateLimiting;

// 429 Too Many Requests
public class TooManyLoginRequests extends RuntimeException {
    public TooManyLoginRequests(String message) {
        super(message);
    }
}
