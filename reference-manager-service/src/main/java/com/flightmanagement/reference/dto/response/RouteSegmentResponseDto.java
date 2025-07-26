package com.flightmanagement.reference.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RouteSegmentResponseDto {

    private Long id;
    private Integer segmentOrder;
    private StationResponseDto originStation;
    private StationResponseDto destinationStation;
    private LocalDateTime scheduledDeparture;
    private LocalDateTime scheduledArrival;
    private Integer distance;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}