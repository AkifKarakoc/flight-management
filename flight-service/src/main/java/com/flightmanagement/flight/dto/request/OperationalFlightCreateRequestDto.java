package com.flightmanagement.flight.dto.request;

import com.flightmanagement.flight.enums.FlightType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class OperationalFlightCreateRequestDto {

    @NotBlank(message = "Flight number is required")
    @Pattern(regexp = "^[A-Z]{2,3}[0-9]{1,4}$", message = "Flight number format invalid")
    private String flightNumber;

    @NotNull(message = "Airline ID is required")
    private Long airlineId;

    @NotNull(message = "Aircraft ID is required")
    private Long aircraftId;

    @NotNull(message = "Flight date is required")
    @FutureOrPresent(message = "Flight date must be today or future")
    private LocalDate flightDate;

    @NotNull(message = "Scheduled departure time is required")
    private LocalTime scheduledDepartureTime;

    @NotNull(message = "Scheduled arrival time is required")
    private LocalTime scheduledArrivalTime;

    @NotNull(message = "Origin station ID is required")
    private Long originStationId;

    @NotNull(message = "Destination station ID is required")
    private Long destinationStationId;

    @Size(max = 10, message = "Gate cannot exceed 10 characters")
    private String gate;

    @Size(max = 5, message = "Terminal cannot exceed 5 characters")
    private String terminal;

    @NotNull(message = "Flight type is required")
    private FlightType flightType;
}