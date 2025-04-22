package com.example.registrationmodule.exception.rateLimiting;

// 429 Too Many Requests
public class TooManyOtpRequests extends RuntimeException {
    public TooManyOtpRequests(String message) {
        super(message);
    }
}
