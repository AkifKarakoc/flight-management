package com.flightmanagement.reference.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AirlineResponseDto {

    private Long id;
    private String code;
    private String name;
    private String country;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}