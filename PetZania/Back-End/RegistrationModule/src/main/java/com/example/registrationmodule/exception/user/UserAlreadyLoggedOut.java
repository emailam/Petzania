package com.example.registrationmodule.exception.user;

public class UserAlreadyLoggedOut extends RuntimeException {
    public UserAlreadyLoggedOut(String message) {
        super(message);
    }
}
