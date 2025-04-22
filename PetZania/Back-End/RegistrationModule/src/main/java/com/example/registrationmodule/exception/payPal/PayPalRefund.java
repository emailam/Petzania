package com.example.registrationmodule.exception.payPal;

// 400 Bad Request
public class PayPalRefund extends RuntimeException {
    public PayPalRefund(String message) {
        super(message);
    }
}