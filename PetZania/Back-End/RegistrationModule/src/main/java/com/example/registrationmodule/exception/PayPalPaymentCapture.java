package com.example.registrationmodule.exception;

// 400 Bad Request
public class PayPalPaymentCapture extends RuntimeException {
    public PayPalPaymentCapture(String message) {
        super(message);
    }
}