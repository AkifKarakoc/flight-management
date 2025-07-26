package com.flightmanagement.reference.dto.response;

import com.flightmanagement.reference.enums.FlightType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class FlightResponseDto {

    private Long id;
    private String flightNumber;
    private AirlineResponseDto airline;
    private AircraftResponseDto aircraft;
    private LocalDate flightDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private FlightType flightType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RouteSegmentResponseDto> segments;
}