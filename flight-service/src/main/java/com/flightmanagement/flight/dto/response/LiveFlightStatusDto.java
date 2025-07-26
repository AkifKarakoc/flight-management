package com.flightmanagement.flight.dto.response;

import com.flightmanagement.flight.enums.FlightStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class LiveFlightStatusDto {

    private Long id;
    private String flightNumber;
    private String airlineCode;
    private String aircraftType;
    private LocalDate flightDate;
    private LocalTime scheduledDepartureTime;
    private LocalTime scheduledArrivalTime;
    private LocalTime actualDepartureTime;
    private LocalTime actualArrivalTime;
    private String originIcaoCode;
    private String destinationIcaoCode;
    private String gate;
    private String terminal;
    private FlightStatus status;
    private Integer departureDelay;
    private Integer arrivalDelay;
    private Boolean isOnTime;
}