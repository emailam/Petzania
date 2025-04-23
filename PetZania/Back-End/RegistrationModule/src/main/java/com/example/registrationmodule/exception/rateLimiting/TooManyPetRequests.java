package com.example.registrationmodule.exception.rateLimiting;

// 429 Too Many Requests
public class TooManyPetRequests extends RuntimeException {
    public TooManyPetRequests(String message) {
        super(message);
    }
}
