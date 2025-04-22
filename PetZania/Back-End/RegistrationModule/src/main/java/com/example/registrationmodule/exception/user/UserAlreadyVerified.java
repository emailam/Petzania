package com.example.registrationmodule.exception.user;

// 409 conflict
public class UserAlreadyVerified extends RuntimeException {
    public UserAlreadyVerified(String message) {
        super(message);
    }
}
