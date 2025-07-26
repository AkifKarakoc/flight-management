package com.flightmanagement.flight.dto.request;

import com.flightmanagement.flight.enums.FlightStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class FlightStatusUpdateRequestDto {

    @NotNull(message = "Status is required")
    private FlightStatus status;

    private LocalTime actualDepartureTime;

    private LocalTime actualArrivalTime;

    @Size(max = 255, message = "Delay reason cannot exceed 255 characters")
    private String delayReason;

    @Size(max = 255, message = "Cancellation reason cannot exceed 255 characters")
    private String cancellationReason;
}