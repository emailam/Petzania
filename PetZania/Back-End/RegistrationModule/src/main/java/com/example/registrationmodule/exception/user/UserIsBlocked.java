package com.example.registrationmodule.exception.user;

public class UserIsBlocked extends RuntimeException {
    public UserIsBlocked(String message) {
        super(message);
    }
}
