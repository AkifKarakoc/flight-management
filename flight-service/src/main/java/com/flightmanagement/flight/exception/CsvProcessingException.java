package com.flightmanagement.flight.exception;

public class CsvProcessingException extends RuntimeException {
    public CsvProcessingException(String message) {
        super(message);
    }

    public CsvProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}