package com.example.registrationmodule.exception;

// 429 Too Many Requests
public class TooManyAdminRequests extends RuntimeException {
    public TooManyAdminRequests(String message) {
        super(message);
    }
}
