package com.flightmanagement.reference.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StationResponseDto {

    private Long id;
    private String icaoCode;
    private String iataCode;
    private String name;
    private String city;
    private String country;
    private String timezone;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}