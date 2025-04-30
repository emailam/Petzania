package com.example.registrationmodule.exception.user;

// 400 bad request

public class UserAccessDenied extends RuntimeException {
    public UserAccessDenied(String message) {
        super(message);
    }
}
