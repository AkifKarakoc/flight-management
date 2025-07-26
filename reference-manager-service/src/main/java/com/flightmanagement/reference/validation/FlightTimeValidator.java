package com.flightmanagement.reference.validation;

import com.flightmanagement.reference.dto.request.FlightCreateRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FlightTimeValidator implements ConstraintValidator<ValidFlightTime, FlightCreateRequestDto> {

    @Override
    public boolean isValid(FlightCreateRequestDto flight, ConstraintValidatorContext context) {
        if (flight.getDepartureTime() == null || flight.getArrivalTime() == null) {
            return true; // Let @NotNull handle null checks
        }

        return flight.getArrivalTime().isAfter(flight.getDepartureTime());
    }
}