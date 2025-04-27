package com.example.registrationmodule.exception.payPal;

// 400 Bad Request
public class PayPalOrderCreation extends RuntimeException {
    public PayPalOrderCreation(String message) {
        super(message);
    }
}