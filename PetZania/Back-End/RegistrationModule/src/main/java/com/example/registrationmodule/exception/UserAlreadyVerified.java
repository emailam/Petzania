package com.example.registrationmodule.exception;

// 409 conflict
public class UserAlreadyVerified extends RuntimeException {
    public UserAlreadyVerified(String message) {
        super(message);
    }
}
