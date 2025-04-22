package com.example.registrationmodule.exception;

// 400 Bad Request
public class UserIdNull extends RuntimeException {
    public UserIdNull(String message) {
        super(message);
    }
}
