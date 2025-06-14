package com.example.registrationmodule.exception.rateLimiting;

public class TooManyPaymentRequests extends RuntimeException {
    public TooManyPaymentRequests(String message) {
        super(message);
    }
}
