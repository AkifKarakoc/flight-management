package com.flightmanagement.reference.dto.request;

import com.flightmanagement.reference.enums.FlightType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class FlightCreateRequestDto {

    @NotBlank(message = "Flight number is required")
    @Pattern(regexp = "^[A-Z]{2,3}[0-9]{1,4}$", message = "Flight number format invalid")
    private String flightNumber;

    @NotNull(message = "Airline ID is required")
    private Long airlineId;

    @NotNull(message = "Aircraft ID is required")
    private Long aircraftId;

    @NotNull(message = "Flight date is required")
    @Future(message = "Flight date must be in the future")
    private LocalDate flightDate;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;

    @NotNull(message = "Flight type is required")
    private FlightType flightType;

    @Valid
    @NotEmpty(message = "At least one route segment is required")
    @Size(max = 5, message = "Maximum 5 route segments allowed")
    private List<RouteSegmentCreateRequestDto> segments;
}