package com.example.registrationmodule.exception;

// 404 not found
public class UserDoesNotExist extends RuntimeException {
    public UserDoesNotExist(String message) {
        super(message);
    }
}
