package com.example.registrationmodule.exception.rateLimiting;


// 429 Too Many Requests
public class TooManyCloudRequests extends RuntimeException {
    public TooManyCloudRequests(String message) {
        super(message);
    }
}

