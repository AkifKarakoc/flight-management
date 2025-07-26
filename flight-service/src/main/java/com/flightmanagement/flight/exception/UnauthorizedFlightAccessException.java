package com.flightmanagement.flight.exception;

public class UnauthorizedFlightAccessException extends RuntimeException {
    public UnauthorizedFlightAccessException(String message) {
        super(message);
    }
}