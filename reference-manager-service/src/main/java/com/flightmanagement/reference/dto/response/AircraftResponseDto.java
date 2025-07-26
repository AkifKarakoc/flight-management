package com.flightmanagement.reference.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AircraftResponseDto {

    private Long id;
    private String type;
    private String manufacturer;
    private String model;
    private Integer capacity;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}