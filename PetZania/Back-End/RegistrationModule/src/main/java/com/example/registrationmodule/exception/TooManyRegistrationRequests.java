package com.example.registrationmodule.exception;

// 429 Too Many Requests
public class TooManyRegistrationRequests extends RuntimeException {
    public TooManyRegistrationRequests(String message) {
        super(message);
    }
}
