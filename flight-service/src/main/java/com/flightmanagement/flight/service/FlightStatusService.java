package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.FlightStatusUpdateRequestDto;
import com.flightmanagement.flight.dto.response.DashboardOverviewDto;
import com.flightmanagement.flight.dto.response.LiveFlightStatusDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.FlightStatus;
import com.flightmanagement.flight.exception.FlightNotFoundException;
import com.flightmanagement.flight.exception.UnauthorizedFlightAccessException;
import com.flightmanagement.flight.mapper.OperationalFlightMapper;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FlightStatusService {

    private final OperationalFlightRepository flightRepository;
    private final OperationalFlightMapper flightMapper;
    private final EventPublishService eventPublishService;
    private final WebSocketService webSocketService;

    public OperationalFlightResponseDto updateFlightStatus(Long id, FlightStatusUpdateRequestDto request, UserContext userContext) {
        OperationalFlight flight = getFlightWithAccessCheck(id, userContext);

        FlightStatus previousStatus = flight.getStatus();

        // Update status and times
        flight.setStatus(request.getStatus());
        if (request.getActualDepartureTime() != null) {
            flight.setActualDepartureTime(request.getActualDepartureTime());

            // Calculate departure delay
            Duration delay = Duration.between(flight.getScheduledDepartureTime(), request.getActualDepartureTime());
            flight.setDepartureDelay((int) delay.toMinutes());
        }

        if (request.getActualArrivalTime() != null) {
            flight.setActualArrivalTime(request.getActualArrivalTime());

            // Calculate arrival delay
            Duration delay = Duration.between(flight.getScheduledArrivalTime(), request.getActualArrivalTime());
            flight.setArrivalDelay((int) delay.toMinutes());
        }

        if (request.getDelayReason() != null) {
            flight.setDelayReason(request.getDelayReason());
        }

        if (request.getCancellationReason() != null) {
            flight.setCancellationReason(request.getCancellationReason());
        }

        flight.setUpdatedBy(userContext.getUsername());
        flight = flightRepository.save(flight);

        // Publish events
        eventPublishService.publishFlightEvent("STATUS_CHANGED", flight, userContext);

        // WebSocket notification
        webSocketService.notifyFlightStatusUpdate(flight, previousStatus);

        log.info("Updated flight status: {} from {} to {}", flight.getFlightNumber(), previousStatus, flight.getStatus());
        return flightMapper.toResponseDto(flight);
    }

    public List<LiveFlightStatusDto> getLiveFlightStatus(LocalDate date, UserContext userContext) {
        List<OperationalFlight> flights;

        if (userContext.isAirlineUser()) {
            flights = flightRepository.findByAirlineIdAndFlightDate(userContext.getAirlineId(), date, null).getContent();
        } else {
            flights = flightRepository.findByFlightDate(date, null).getContent();
        }

        return flights.stream()
                .map(this::mapToLiveStatusDto)
                .toList();
    }

    public DashboardOverviewDto getDashboardOverview(LocalDate date, UserContext userContext) {
        List<OperationalFlight> flights;

        if (userContext.isAirlineUser()) {
            flights = flightRepository.findByAirlineIdAndFlightDate(userContext.getAirlineId(), date, null).getContent();
        } else {
            flights = flightRepository.findByFlightDate(date, null).getContent();
        }

        return calculateDashboardMetrics(flights, date);
    }

    private OperationalFlight getFlightWithAccessCheck(Long id, UserContext userContext) {
        OperationalFlight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found with id: " + id));

        if (userContext.isAirlineUser() && !flight.getAirlineId().equals(userContext.getAirlineId())) {
            throw new UnauthorizedFlightAccessException("Cannot access flights from different airline");
        }

        return flight;
    }

    private LiveFlightStatusDto mapToLiveStatusDto(OperationalFlight flight) {
        LiveFlightStatusDto dto = new LiveFlightStatusDto();
        dto.setId(flight.getId());
        dto.setFlightNumber(flight.getFlightNumber());
        dto.setAirlineCode(flight.getAirlineCode());
        dto.setAircraftType(flight.getAircraftType());
        dto.setFlightDate(flight.getFlightDate());
        dto.setScheduledDepartureTime(flight.getScheduledDepartureTime());
        dto.setScheduledArrivalTime(flight.getScheduledArrivalTime());
        dto.setActualDepartureTime(flight.getActualDepartureTime());
        dto.setActualArrivalTime(flight.getActualArrivalTime());
        dto.setOriginIcaoCode(flight.getOriginIcaoCode());
        dto.setDestinationIcaoCode(flight.getDestinationIcaoCode());
        dto.setGate(flight.getGate());
        dto.setTerminal(flight.getTerminal());
        dto.setStatus(flight.getStatus());
        dto.setDepartureDelay(flight.getDepartureDelay());
        dto.setArrivalDelay(flight.getArrivalDelay());
        dto.setIsOnTime(flight.getArrivalDelay() != null ? flight.getArrivalDelay() <= 15 : null);
        return dto;
    }

    private DashboardOverviewDto calculateDashboardMetrics(List<OperationalFlight> flights, LocalDate date) {
        int totalFlights = flights.size();
        int scheduledFlights = (int) flights.stream().filter(f -> f.getStatus() == FlightStatus.SCHEDULED).count();
        int departedFlights = (int) flights.stream().filter(f -> f.getStatus() == FlightStatus.DEPARTED).count();
        int arrivedFlights = (int) flights.stream().filter(f -> f.getStatus() == FlightStatus.ARRIVED).count();
        int delayedFlights = (int) flights.stream().filter(f -> f.getStatus() == FlightStatus.DELAYED).count();
        int cancelledFlights = (int) flights.stream().filter(f -> f.getStatus() == FlightStatus.CANCELLED).count();

        double averageDelay = flights.stream()
                .filter(f -> f.getArrivalDelay() != null && f.getArrivalDelay() > 0)
                .mapToInt(OperationalFlight::getArrivalDelay)
                .average()
                .orElse(0.0);

        int onTimeFlights = (int) flights.stream()
                .filter(f -> f.getArrivalDelay() != null && f.getArrivalDelay() <= 15)
                .count();

        double onTimePerformance = totalFlights > 0 ? (double) onTimeFlights / totalFlights * 100 : 0.0;

        return DashboardOverviewDto.builder()
                .date(date)
                .totalFlights(totalFlights)
                .scheduledFlights(scheduledFlights)
                .departedFlights(departedFlights)
                .arrivedFlights(arrivedFlights)
                .delayedFlights(delayedFlights)
                .cancelledFlights(cancelledFlights)
                .averageDelay(averageDelay)
                .onTimePerformance(onTimePerformance)
                .activeAirlines((int) flights.stream().map(OperationalFlight::getAirlineId).distinct().count())
                .busiestRoute("LTBA-EDDF") // This would be calculated from actual data
                .topDelayReasons(List.of("Weather", "ATC", "Technical"))
                .build();
    }
}