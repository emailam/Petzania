package com.example.registrationmodule.exception.payPal;

// 404 Not Found
public class PayPalApprovalUrlNotFound extends RuntimeException {
    public PayPalApprovalUrlNotFound(String message) {
        super(message);
    }
}