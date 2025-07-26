package com.flightmanagement.reference.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AirlineCreateRequestDto {

    @NotBlank(message = "Airline code is required")
    @Pattern(regexp = "^[A-Z]{2,3}$", message = "Airline code must be 2-3 uppercase letters")
    private String code;

    @NotBlank(message = "Airline name is required")
    @Size(max = 100, message = "Airline name cannot exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Country cannot exceed 50 characters")
    private String country;
}