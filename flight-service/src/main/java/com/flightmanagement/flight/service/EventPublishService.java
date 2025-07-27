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

    public void publishFlightEvent(String eventType, OperationalFlight flight, UserContext userContext) {
        try {
            FlightEventDto event = FlightEventDto.builder()
                    .eventType(eventType)
                    .entityType("OPERATIONAL_FLIGHT")
                    .entityId(flight.getId())
                    .flightNumber(flight.getFlightNumber())
                    .airlineId(flight.getAirlineId())
                    .flightDate(flight.getFlightDate())
                    .timestamp(LocalDateTime.now())
                    .triggeredBy(userContext.getUsername())
                    .build();

            kafkaTemplate.send(properties.getKafka().getTopics().getFlightEvents(), 
                             flight.getId().toString(), event);

            log.debug("Published flight event: {} for flight: {}", eventType, flight.getFlightNumber());
        } catch (Exception e) {
            log.error("Failed to publish flight event: {} for flight: {}", eventType, flight.getFlightNumber(), e);
        }
    }

    public void publishFlightEvent(String eventType, OperationalFlight flight, UserContext userContext, 
                                 String changeType, Map<String, Object> changedFields) {
        try {
            FlightEventDto event = FlightEventDto.builder()
                    .eventType(eventType)
                    .entityType("OPERATIONAL_FLIGHT")
                    .entityId(flight.getId())
                    .flightNumber(flight.getFlightNumber())
                    .airlineId(flight.getAirlineId())
                    .flightDate(flight.getFlightDate())
                    .changeType(changeType)
                    .currentData(changedFields)
                    .timestamp(LocalDateTime.now())
                    .triggeredBy(userContext.getUsername())
                    .build();

            kafkaTemplate.send(properties.getKafka().getTopics().getFlightEvents(), 
                             flight.getId().toString(), event);

            log.debug("Published flight event: {} for flight: {} with changes: {}", 
                     eventType, flight.getFlightNumber(), changedFields);
        } catch (Exception e) {
            log.error("Failed to publish flight event: {} for flight: {}", eventType, flight.getFlightNumber(), e);
        }
    }

    public void publishUploadEvent(String eventType, Long batchId, UserContext userContext) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("entityType", "UPLOAD_BATCH");
            event.put("entityId", batchId);
            event.put("timestamp", LocalDateTime.now());
            event.put("triggeredBy", userContext.getUsername());

            kafkaTemplate.send(properties.getKafka().getTopics().getUploadEvents(), 
                             batchId.toString(), event);

            log.debug("Published upload event: {} for batch: {}", eventType, batchId);
        } catch (Exception e) {
            log.error("Failed to publish upload event: {} for batch: {}", eventType, batchId, e);
        }
    }

    public void publishUploadEvent(String eventType, Long batchId, UserContext userContext, 
                                 Map<String, Object> additionalData) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("entityType", "UPLOAD_BATCH");
            event.put("entityId", batchId);
            event.put("timestamp", LocalDateTime.now());
            event.put("triggeredBy", userContext.getUsername());
            event.putAll(additionalData);

            kafkaTemplate.send(properties.getKafka().getTopics().getUploadEvents(), 
                             batchId.toString(), event);

            log.debug("Published upload event: {} for batch: {} with data: {}", 
                     eventType, batchId, additionalData);
        } catch (Exception e) {
            log.error("Failed to publish upload event: {} for batch: {}", eventType, batchId, e);
        }
    }
}