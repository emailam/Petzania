package com.example.adoption_and_breeding_module.exception;

// 404 not found
public class UserNotFound extends RuntimeException {
    public UserNotFound(String message) {
        super(message);
    }
}
