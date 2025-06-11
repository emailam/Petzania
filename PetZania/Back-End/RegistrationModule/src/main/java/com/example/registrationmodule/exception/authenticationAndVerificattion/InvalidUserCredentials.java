package com.example.registrationmodule.exception.authenticationAndVerificattion;

public class InvalidUserCredentials extends RuntimeException {
    public InvalidUserCredentials(String message) {
        super(message);
    }
}
