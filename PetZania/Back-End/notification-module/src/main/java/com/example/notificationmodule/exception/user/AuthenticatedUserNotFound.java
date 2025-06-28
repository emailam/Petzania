package com.example.notificationmodule.exception.user;

public class AuthenticatedUserNotFound extends RuntimeException {
    public AuthenticatedUserNotFound(String message) {
        super(message);
    }
}

