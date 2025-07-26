package com.flightmanagement.reference.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StationCreateRequestDto {

    @NotBlank(message = "ICAO code is required")
    @Pattern(regexp = "^[A-Z]{4}$", message = "ICAO code must be 4 uppercase letters")
    private String icaoCode;

    @Pattern(regexp = "^[A-Z]{3}$", message = "IATA code must be 3 uppercase letters")
    private String iataCode;

    @NotBlank(message = "Station name is required")
    @Size(max = 100, message = "Station name cannot exceed 100 characters")
    private String name;

    @Size(max = 50, message = "City cannot exceed 50 characters")
    private String city;

    @Size(max = 50, message = "Country cannot exceed 50 characters")
    private String country;

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone;
}