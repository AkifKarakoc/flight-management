package com.flightmanagement.reference.exception;

public class ReferenceNotFoundException extends RuntimeException {
    public ReferenceNotFoundException(String message) {
        super(message);
    }
}