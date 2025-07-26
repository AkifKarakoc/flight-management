package com.flightmanagement.flight.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.reference-manager.base-url}")
    private String referenceManagerBaseUrl;

    @CircuitBreaker(name = "reference-manager", fallbackMethod = "getAirlineFromCache")
    @Cacheable(value = "airlines", key = "#airlineId")
    public Map<String, Object> getAirline(Long airlineId) {
        try {
            String url = referenceManagerBaseUrl + "/api/v1/airlines/" + airlineId;
            Map<String, Object> airline = restTemplate.getForObject(url, Map.class);

            // Cache result
            redisTemplate.opsForValue().set("airline:" + airlineId, airline, Duration.ofHours(2));
            return airline;

        } catch (Exception e) {
            log.error("Failed to fetch airline data for ID: {}", airlineId, e);
            return getAirlineFromCache(airlineId, e);
        }
    }

    @CircuitBreaker(name = "reference-manager", fallbackMethod = "getStationFromCache")
    @Cacheable(value = "stations", key = "#stationId")
    public Map<String, Object> getStation(Long stationId) {
        try {
            String url = referenceManagerBaseUrl + "/api/v1/stations/" + stationId;
            Map<String, Object> station = restTemplate.getForObject(url, Map.class);

            redisTemplate.opsForValue().set("station:" + stationId, station, Duration.ofHours(4));
            return station;

        } catch (Exception e) {
            log.error("Failed to fetch station data for ID: {}", stationId, e);
            return getStationFromCache(stationId, e);
        }
    }

    @CircuitBreaker(name = "reference-manager", fallbackMethod = "getAircraftFromCache")
    @Cacheable(value = "aircraft", key = "#aircraftId")
    public Map<String, Object> getAircraft(Long aircraftId) {
        try {
            String url = referenceManagerBaseUrl + "/api/v1/aircraft/" + aircraftId;
            Map<String, Object> aircraft = restTemplate.getForObject(url, Map.class);

            redisTemplate.opsForValue().set("aircraft:" + aircraftId, aircraft, Duration.ofHours(1));
            return aircraft;

        } catch (Exception e) {
            log.error("Failed to fetch aircraft data for ID: {}", aircraftId, e);
            return getAircraftFromCache(aircraftId, e);
        }
    }

    // Fallback methods
    public Map<String, Object> getAirlineFromCache(Long airlineId, Exception ex) {
        return (Map<String, Object>) redisTemplate.opsForValue().get("airline:" + airlineId);
    }

    public Map<String, Object> getStationFromCache(Long stationId, Exception ex) {
        return (Map<String, Object>) redisTemplate.opsForValue().get("station:" + stationId);
    }

    public Map<String, Object> getAircraftFromCache(Long aircraftId, Exception ex) {
        return (Map<String, Object>) redisTemplate.opsForValue().get("aircraft:" + aircraftId);
    }
}