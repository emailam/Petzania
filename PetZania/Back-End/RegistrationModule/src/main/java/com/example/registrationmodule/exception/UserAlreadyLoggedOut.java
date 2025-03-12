package com.example.registrationmodule.exception;

public class UserAlreadyLoggedOut extends RuntimeException {
    public UserAlreadyLoggedOut(String message) {
        super(message);
    }
}
