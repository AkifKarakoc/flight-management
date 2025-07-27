package com.flightmanagement.flight.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class ReferenceDataEventListener {

    private final CacheManager cacheManager;

    @KafkaListener(topics = "reference.events", groupId = "flight-service-group")
    public void handleReferenceEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            String entityType = (String) event.get("entityType");
            Long entityId = ((Number) event.get("entityId")).longValue();

            log.info("Received reference event: {} for {} with id: {}", eventType, entityType, entityId);

            // Invalidate cache based on entity type
            switch (entityType) {
                case "AIRLINE":
                    invalidateCache("airlines", entityId);
                    break;
                case "STATION":
                    invalidateCache("stations", entityId);
                    break;
                case "AIRCRAFT":
                    invalidateCache("aircraft", entityId);
                    break;
            }

        } catch (Exception e) {
            log.error("Failed to process reference event", e);
        }
    }

    private void invalidateCache(String cacheName, Long entityId) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(entityId);
            log.debug("Evicted cache entry: {}:{}", cacheName, entityId);
        }
    }
}