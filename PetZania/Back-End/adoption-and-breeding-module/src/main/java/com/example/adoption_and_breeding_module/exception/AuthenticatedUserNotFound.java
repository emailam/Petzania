package com.example.adoption_and_breeding_module.exception;

// 404
public class AuthenticatedUserNotFound extends RuntimeException {
    public AuthenticatedUserNotFound(String message) {
        super(message);
    }
}
