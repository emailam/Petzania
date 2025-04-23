package com.example.registrationmodule.exception.user;

// 409 conflict
public class UsernameAlreadyExists extends RuntimeException {
    public UsernameAlreadyExists(String message) {
        super(message);
    }
}
