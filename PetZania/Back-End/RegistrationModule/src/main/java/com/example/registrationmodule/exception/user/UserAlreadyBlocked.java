package com.example.registrationmodule.exception.user;

public class UserAlreadyBlocked extends RuntimeException {
    public UserAlreadyBlocked(String message) {
        super(message);
    }
}
