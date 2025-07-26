package com.flightmanagement.flight.dto.websocket;

import com.flightmanagement.flight.enums.FlightStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class FlightStatusUpdateMessage {

    private Long flightId;
    private String flightNumber;
    private FlightStatus previousStatus;
    private FlightStatus currentStatus;
    private LocalTime actualTime;
    private Integer delay;
    private String eventType;
    private LocalDateTime timestamp;
}