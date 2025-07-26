package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.kafka.FlightEventDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublishService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.flight-events}")
    private String flightEventsTopic;

    @Async
    public void publishFlightEvent(String eventType, OperationalFlight flight, UserContext userContext) {
        try {
            FlightEventDto event = FlightEventDto.builder()
                    .eventType(eventType)
                    .entityType("OPERATIONAL_FLIGHT")
                    .entityId(flight.getId())
                    .flightNumber(flight.getFlightNumber())
                    .airlineId(flight.getAirlineId())
                    .flightDate(flight.getFlightDate())
                    .currentData(mapFlightToEventData(flight))
                    .timestamp(LocalDateTime.now())
                    .triggeredBy(userContext.getUsername())
                    .build();

            kafkaTemplate.send(flightEventsTopic, flight.getId().toString(), event);
            log.info("Published flight event: {} for flight: {}", eventType, flight.getFlightNumber());

        } catch (Exception e) {
            log.error("Failed to publish flight event", e);
        }
    }

    private Object mapFlightToEventData(OperationalFlight flight) {
        return Map.of(
                "id", flight.getId(),
                "flightNumber", flight.getFlightNumber(),
                "airlineId", flight.getAirlineId(),
                "aircraftId", flight.getAircraftId(),
                "flightDate", flight.getFlightDate(),
                "status", flight.getStatus(),
                "version", flight.getVersion()
        );
    }
}