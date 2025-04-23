package com.example.registrationmodule.exception.payPal;

// 404 Not Found
public class PayPalOrderNotFound extends RuntimeException {
    public PayPalOrderNotFound(String message) {
        super(message);
    }
}