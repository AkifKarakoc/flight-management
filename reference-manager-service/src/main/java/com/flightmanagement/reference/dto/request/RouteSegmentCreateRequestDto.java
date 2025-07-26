package com.flightmanagement.reference.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RouteSegmentCreateRequestDto {

    @NotNull(message = "Segment order is required")
    @Min(value = 1, message = "Segment order must be at least 1")
    @Max(value = 5, message = "Segment order cannot exceed 5")
    private Integer segmentOrder;

    @NotNull(message = "Origin station ID is required")
    private Long originStationId;

    @NotNull(message = "Destination station ID is required")
    private Long destinationStationId;

    @NotNull(message = "Scheduled departure is required")
    @Future(message = "Scheduled departure must be in the future")
    private LocalDateTime scheduledDeparture;

    @NotNull(message = "Scheduled arrival is required")
    @Future(message = "Scheduled arrival must be in the future")
    private LocalDateTime scheduledArrival;

    @Min(value = 1, message = "Distance must be at least 1 nautical mile")
    private Integer distance;
}