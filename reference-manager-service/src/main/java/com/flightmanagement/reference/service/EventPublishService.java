package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.kafka.ReferenceEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublishService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.reference-events}")
    private String referenceEventsTopic;

    @Async
    public void publishReferenceEvent(String eventType, String entityType, Long entityId, Long airlineId) {
        try {
            ReferenceEventDto event = ReferenceEventDto.builder()
                    .eventType(eventType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .airlineId(airlineId)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(referenceEventsTopic, entityId.toString(), event);
            log.info("Published reference event: {} for entity: {} with id: {}", eventType, entityType, entityId);

        } catch (Exception e) {
            log.error("Failed to publish reference event", e);
        }
    }
}