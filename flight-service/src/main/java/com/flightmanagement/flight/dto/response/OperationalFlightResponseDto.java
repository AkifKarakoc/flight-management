package com.flightmanagement.flight.dto.response;

import com.flightmanagement.flight.enums.FlightStatus;
import com.flightmanagement.flight.enums.FlightType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class OperationalFlightResponseDto {

    private Long id;
    private String flightNumber;
    private String airlineCode;
    private String airlineName;
    private String aircraftType;
    private LocalDate flightDate;
    private LocalTime scheduledDepartureTime;
    private LocalTime scheduledArrivalTime;
    private LocalTime actualDepartureTime;
    private LocalTime actualArrivalTime;
    private Integer departureDelay;
    private Integer arrivalDelay;
    private String originIcaoCode;
    private String destinationIcaoCode;
    private String gate;
    private String terminal;
    private FlightStatus status;
    private FlightType flightType;
    private Integer version;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}