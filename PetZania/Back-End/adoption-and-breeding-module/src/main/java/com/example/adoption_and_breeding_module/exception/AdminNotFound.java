package com.example.adoption_and_breeding_module.exception;

// 404 not found
public class AdminNotFound extends RuntimeException {
    public AdminNotFound(String message) {
        super(message);
    }
}
