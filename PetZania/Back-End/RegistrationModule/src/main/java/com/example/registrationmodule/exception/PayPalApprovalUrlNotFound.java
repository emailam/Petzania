package com.example.registrationmodule.exception;

// 404 Not Found
public class PayPalApprovalUrlNotFound extends RuntimeException {
    public PayPalApprovalUrlNotFound(String message) {
        super(message);
    }
}