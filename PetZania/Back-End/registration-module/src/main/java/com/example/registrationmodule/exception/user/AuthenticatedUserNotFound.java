package com.example.registrationmodule.exception.user;

// 404 not found

public class AuthenticatedUserNotFound extends RuntimeException {
    public AuthenticatedUserNotFound(String message) {
        super(message);
    }
}
