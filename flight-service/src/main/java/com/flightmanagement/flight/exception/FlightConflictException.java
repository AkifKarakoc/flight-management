package com.flightmanagement.flight.exception;

import com.flightmanagement.flight.service.ConflictDetectionService;

import java.util.List;

public class FlightConflictException extends RuntimeException {

    private final List<ConflictDetectionService.Conflict> conflicts;

    public FlightConflictException(String message, List<ConflictDetectionService.Conflict> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    public List<ConflictDetectionService.Conflict> getConflicts() {
        return conflicts;
    }
}