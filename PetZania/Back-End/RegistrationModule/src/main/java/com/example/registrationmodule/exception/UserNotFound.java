package com.example.registrationmodule.exception;

// 404 not found
public class UserNotFound extends RuntimeException {
    public UserNotFound(String message) {
        super(message);
    }
}
