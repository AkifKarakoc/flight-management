package com.flightmanagement.flight.service;

import com.flightmanagement.flight.config.FlightServiceProperties;
import com.flightmanagement.flight.dto.kafka.FlightEventDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublishService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FlightServiceProperties properties;

    @Async("eventPublishingExecutor")
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

            String topic = properties.getKafka().getTopics().getFlightEvents();
            kafkaTemplate.send(topic, flight.getId().toString(), event);

            log.info("Published flight event: {} for flight: {} to topic: {}",
                    eventType, flight.getFlightNumber(), topic);

        } catch (Exception e) {
            log.error("Failed to publish flight event: {} for flight: {}",
                    eventType, flight.getFlightNumber(), e);
        }
    }

    @Async("eventPublishingExecutor")
    public void publishFlightStatusEvent(String eventType, OperationalFlight flight,
                                         OperationalFlight previousState, UserContext userContext) {
        try {
            FlightEventDto event = FlightEventDto.builder()
                    .eventType(eventType)
                    .entityType("OPERATIONAL_FLIGHT")
                    .entityId(flight.getId())
                    .flightNumber(flight.getFlightNumber())
                    .airlineId(flight.getAirlineId())
                    .flightDate(flight.getFlightDate())
                    .changeType("STATUS_UPDATE")
                    .previousData(previousState != null ? mapFlightToEventData(previousState) : null)
                    .currentData(mapFlightToEventData(flight))
                    .timestamp(LocalDateTime.now())
                    .triggeredBy(userContext.getUsername())
                    .build();

            String topic = properties.getKafka().getTopics().getFlightEvents();
            kafkaTemplate.send(topic, flight.getId().toString(), event);

            log.info("Published flight status event: {} for flight: {}",
                    eventType, flight.getFlightNumber());

        } catch (Exception e) {
            log.error("Failed to publish flight status event", e);
        }
    }

    private Map<String, Object> mapFlightToEventData(OperationalFlight flight) {
        Map<String, Object> data = new HashMap<>();

        // Basic flight info
        data.put("id", flight.getId());
        data.put("flightNumber", flight.getFlightNumber());
        data.put("airlineId", flight.getAirlineId());
        data.put("airlineCode", flight.getAirlineCode() != null ? flight.getAirlineCode() : "");
        data.put("airlineName", flight.getAirlineName() != null ? flight.getAirlineName() : "");

        // Aircraft info
        data.put("aircraftId", flight.getAircraftId());
        data.put("aircraftType", flight.getAircraftType() != null ? flight.getAircraftType() : "");

        // Date and time info
        data.put("flightDate", flight.getFlightDate());
        data.put("scheduledDepartureTime", flight.getScheduledDepartureTime());
        data.put("scheduledArrivalTime", flight.getScheduledArrivalTime());
        data.put("actualDepartureTime", flight.getActualDepartureTime());
        data.put("actualArrivalTime", flight.getActualArrivalTime());

        // Delay info
        data.put("departureDelay", flight.getDepartureDelay());
        data.put("arrivalDelay", flight.getArrivalDelay());

        // Station info
        data.put("originStationId", flight.getOriginStationId());
        data.put("originIcaoCode", flight.getOriginIcaoCode() != null ? flight.getOriginIcaoCode() : "");
        data.put("destinationStationId", flight.getDestinationStationId());
        data.put("destinationIcaoCode", flight.getDestinationIcaoCode() != null ? flight.getDestinationIcaoCode() : "");

        // Gate and terminal
        data.put("gate", flight.getGate());
        data.put("terminal", flight.getTerminal());

        // Status and type
        data.put("status", flight.getStatus());
        data.put("flightType", flight.getFlightType());

        // Reasons
        data.put("cancellationReason", flight.getCancellationReason());
        data.put("delayReason", flight.getDelayReason());

        // Metadata
        data.put("version", flight.getVersion());
        data.put("uploadBatchId", flight.getUploadBatchId());
        data.put("isActive", flight.getIsActive());
        data.put("createdAt", flight.getCreatedAt());
        data.put("updatedAt", flight.getUpdatedAt());
        data.put("createdBy", flight.getCreatedBy());
        data.put("updatedBy", flight.getUpdatedBy());

        return data;
    }
}