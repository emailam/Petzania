package com.example.adoption_and_breeding_module.exception;

// 403 forbidden

public class UserAccessDenied extends RuntimeException {
    public UserAccessDenied(String message) {
        super(message);
    }
}
