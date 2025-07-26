package com.flightmanagement.reference.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AircraftCreateRequestDto {

    @NotBlank(message = "Aircraft type is required")
    @Size(max = 20, message = "Aircraft type cannot exceed 20 characters")
    private String type;

    @Size(max = 50, message = "Manufacturer cannot exceed 50 characters")
    private String manufacturer;

    @Size(max = 50, message = "Model cannot exceed 50 characters")
    private String model;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}