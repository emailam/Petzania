package com.example.registrationmodule.exception.pet;

// 404 not found
public class PetNotFound extends RuntimeException {
    public PetNotFound(String message) {
        super(message);
    }
}
