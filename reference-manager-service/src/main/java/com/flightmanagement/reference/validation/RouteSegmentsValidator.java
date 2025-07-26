package com.flightmanagement.reference.validation;

import com.flightmanagement.reference.dto.request.FlightCreateRequestDto;
import com.flightmanagement.reference.dto.request.RouteSegmentCreateRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class RouteSegmentsValidator implements ConstraintValidator<ValidRouteSegments, FlightCreateRequestDto> {

    @Override
    public boolean isValid(FlightCreateRequestDto flight, ConstraintValidatorContext context) {
        List<RouteSegmentCreateRequestDto> segments = flight.getSegments();

        if (segments == null || segments.isEmpty()) {
            return true;
        }

        // Sort by segment order
        segments.sort((s1, s2) -> Integer.compare(s1.getSegmentOrder(), s2.getSegmentOrder()));

        // Validate segment connectivity and timing
        for (int i = 0; i < segments.size(); i++) {
            RouteSegmentCreateRequestDto current = segments.get(i);

            // Check segment order starts from 1 and is sequential
            if (current.getSegmentOrder() != i + 1) {
                addConstraintViolation(context, "Segment order must be sequential starting from 1");
                return false;
            }

            // Check origin != destination
            if (current.getOriginStationId().equals(current.getDestinationStationId())) {
                addConstraintViolation(context, "Origin and destination stations cannot be the same");
                return false;
            }

            // Check arrival > departure
            if (current.getScheduledArrival().isBefore(current.getScheduledDeparture())) {
                addConstraintViolation(context, "Segment arrival time must be after departure time");
                return false;
            }

            // Check connectivity with next segment
            if (i < segments.size() - 1) {
                RouteSegmentCreateRequestDto next = segments.get(i + 1);

                if (!current.getDestinationStationId().equals(next.getOriginStationId())) {
                    addConstraintViolation(context, "Segments must be connected: segment " + (i+1) + " destination must equal segment " + (i+2) + " origin");
                    return false;
                }

                // Check transfer time (at least 30 minutes)
                if (next.getScheduledDeparture().isBefore(current.getScheduledArrival().plusMinutes(30))) {
                    addConstraintViolation(context, "Minimum 30 minutes transfer time required between segments");
                    return false;
                }
            }
        }

        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}