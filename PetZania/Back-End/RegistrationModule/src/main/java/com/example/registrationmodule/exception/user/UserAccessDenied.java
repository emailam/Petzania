package com.example.registrationmodule.exception.user;

// 403 forbidden

public class UserAccessDenied extends RuntimeException {
    public UserAccessDenied(String message) {
        super(message);
    }
}
