package com.flightmanagement.flight.dto.request;

import com.flightmanagement.flight.enums.FlightStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FlightSearchCriteria {

    private String flightNumber;
    private Long airlineId;
    private LocalDate flightDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private FlightStatus status;
    private String originIcaoCode;
    private String destinationIcaoCode;
    private int page = 0;
    private int size = 20;
}