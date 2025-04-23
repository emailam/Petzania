package com.example.registrationmodule.exception.payPal;

// 400 Bad Request
public class PayPalPaymentCapture extends RuntimeException {
    public PayPalPaymentCapture(String message) {
        super(message);
    }
}