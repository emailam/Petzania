package com.example.registrationmodule.exception;

// 404 Not Found
public class PayPalOrderNotFound extends RuntimeException {
    public PayPalOrderNotFound(String message) {
        super(message);
    }
}