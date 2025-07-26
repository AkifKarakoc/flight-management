package com.flightmanagement.reference.dto.request;

import com.flightmanagement.reference.enums.FlightType;
import com.flightmanagement.reference.validation.ValidFlightTime;
import com.flightmanagement.reference.validation.ValidRouteSegments;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@ValidFlightTime
@ValidRouteSegments
@Schema(description = "Flight creation request with route segments")
public class FlightCreateRequestDto {

    @NotBlank(message = "Flight number is required")
    @Pattern(regexp = "^[A-Z]{2,3}[0-9]{1,4}$", message = "Flight number format invalid")
    @Schema(description = "Flight number (e.g., TK123, LH456)", example = "TK123")
    private String flightNumber;

    @NotNull(message = "Airline ID is required")
    @Schema(description = "Airline identifier", example = "1")
    private Long airlineId;

    @NotNull(message = "Aircraft ID is required")
    @Schema(description = "Aircraft identifier", example = "1")
    private Long aircraftId;

    @NotNull(message = "Flight date is required")
    @Future(message = "Flight date must be in the future")
    @Schema(description = "Flight date", example = "2025-08-15")
    private LocalDate flightDate;

    @NotNull(message = "Departure time is required")
    @Schema(description = "Scheduled departure time", example = "14:30")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    @Schema(description = "Scheduled arrival time", example = "18:45")
    private LocalTime arrivalTime;

    @NotNull(message = "Flight type is required")
    @Schema(description = "Type of flight", example = "PASSENGER")
    private FlightType flightType;

    @Valid
    @NotEmpty(message = "At least one route segment is required")
    @Size(max = 5, message = "Maximum 5 route segments allowed")
    @Schema(description = "Flight route segments (max 5)")
    private List<RouteSegmentCreateRequestDto> segments;
}