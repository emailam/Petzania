package com.example.registrationmodule.exception;


public class PayPalOrderCreation extends RuntimeException {
    public PayPalOrderCreation(String message) {
        super(message);
    }
}