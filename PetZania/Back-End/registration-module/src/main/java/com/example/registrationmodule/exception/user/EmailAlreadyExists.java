package com.example.registrationmodule.exception.user;

// 409 conflict
public class EmailAlreadyExists extends RuntimeException {
    public EmailAlreadyExists(String message) {
        super(message);
    }
}
