package com.example.registrationmodule.exception.user;

public class UserAlreadyUnblocked extends RuntimeException {
    public UserAlreadyUnblocked(String message) {
        super(message);
    }
}
