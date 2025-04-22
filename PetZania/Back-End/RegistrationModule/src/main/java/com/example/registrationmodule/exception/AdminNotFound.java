package com.example.registrationmodule.exception;

// 404 not found
public class AdminNotFoundException extends RuntimeException {
    public AdminNotFoundException(String message) {
        super(message);
    }
}