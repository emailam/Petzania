package com.example.registrationmodule.exception.user;

public class UserNotVerified extends RuntimeException {
    public UserNotVerified(String message) {
        super(message);
    }
}
