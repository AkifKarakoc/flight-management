package com.flightmanagement.flight.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DashboardOverviewDto {

    private LocalDate date;
    private Integer totalFlights;
    private Integer scheduledFlights;
    private Integer departedFlights;
    private Integer arrivedFlights;
    private Integer delayedFlights;
    private Integer cancelledFlights;
    private Double averageDelay;
    private Double onTimePerformance;
    private Integer activeAirlines;
    private String busiestRoute;
    private List<String> topDelayReasons;
}