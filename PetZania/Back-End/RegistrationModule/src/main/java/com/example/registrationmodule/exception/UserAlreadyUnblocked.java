package com.example.registrationmodule.exception;

public class UserAlreadyUnblocked extends RuntimeException {
    public UserAlreadyUnblocked(String message) {
        super(message);
    }
}
