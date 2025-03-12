package com.example.registrationmodule.exception;

public class UserIsBlocked extends RuntimeException {
    public UserIsBlocked(String message) {
        super(message);
    }
}
