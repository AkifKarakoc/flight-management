package com.flightmanagement.flight.dto.kafka;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class FlightEventDto {

    private String eventType;
    private String entityType;
    private Long entityId;
    private String flightNumber;
    private Long airlineId;
    private LocalDate flightDate;
    private String changeType;
    private Object previousData;
    private Object currentData;
    private LocalDateTime timestamp;
    private String triggeredBy;
}