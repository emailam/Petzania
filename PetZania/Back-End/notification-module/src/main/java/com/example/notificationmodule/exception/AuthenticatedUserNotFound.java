package com.example.notificationmodule.exception;

public class AuthenticatedUserNotFound extends RuntimeException {
    public AuthenticatedUserNotFound(String message) {
        super(message);
    }
}

