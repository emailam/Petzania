package com.example.registrationmodule.exception.authenticationAndVerificattion;

public class RefreshTokenNotValid extends RuntimeException {
    public RefreshTokenNotValid(String message) {
        super(message);
    }
}
