package com.example.registrationmodule.exception;

public class UserNotVerified extends RuntimeException {
    public UserNotVerified(String message) {
        super(message);
    }
}
