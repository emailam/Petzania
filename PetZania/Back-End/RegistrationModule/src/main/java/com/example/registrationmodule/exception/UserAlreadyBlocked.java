package com.example.registrationmodule.exception;

public class UserAlreadyBlocked extends RuntimeException {
    public UserAlreadyBlocked(String message) {
        super(message);
    }
}
