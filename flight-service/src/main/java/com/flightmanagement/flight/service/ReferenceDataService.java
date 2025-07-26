package com.flightmanagement.flight.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${app.reference-manager.base-url}")
    private String referenceManagerBaseUrl;

    @CircuitBreaker(name = "reference-manager", fallbackMethod = "getAirlineFromCache")
    @Cacheable(value = "airlines", key = "#airlineId")
    public Map<String, Object> getAirline(Long airlineId) {
        try {
            String url = referenceManagerBaseUrl + "/api/v1/airlines/" + airlineId;
            log.debug("Fetching airline data from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> airline = response.getBody();

                // Cache with longer TTL
                redisTemplate.opsForValue().set("airline:" + airlineId, airline, Duration.ofHours(4));
                log.debug("Cached airline data for ID: {}", airlineId);

                return airline;
            }

            throw new RuntimeException("Failed to fetch airline data");

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
            log.debug("Fetching station data from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> station = response.getBody();

                redisTemplate.opsForValue().set("station:" + stationId, station, Duration.ofHours(8));
                log.debug("Cached station data for ID: {}", stationId);

                return station;
            }

            throw new RuntimeException("Failed to fetch station data");

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
            log.debug("Fetching aircraft data from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> aircraft = response.getBody();

                redisTemplate.opsForValue().set("aircraft:" + aircraftId, aircraft, Duration.ofHours(2));
                log.debug("Cached aircraft data for ID: {}", aircraftId);

                return aircraft;
            }

            throw new RuntimeException("Failed to fetch aircraft data");

        } catch (Exception e) {
            log.error("Failed to fetch aircraft data for ID: {}", aircraftId, e);
            return getAircraftFromCache(aircraftId, e);
        }
    }

    // New method: Resolve airline by code
    @CircuitBreaker(name = "reference-manager", fallbackMethod = "getAirlineByCodeFromCache")
    public Optional<Map<String, Object>> getAirlineByCode(String airlineCode) {
        try {
            String url = referenceManagerBaseUrl + "/api/v1/airlines?code=" + airlineCode;
            log.debug("Fetching airline by code from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> pageResponse = response.getBody();
                List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");

                if (!content.isEmpty()) {
                    Map<String, Object> airline = content.get(0);
                    Long airlineId = ((Number) airline.get("id")).longValue();

                    // Cache both by ID and code
                    redisTemplate.opsForValue().set("airline:" + airlineId, airline, Duration.ofHours(4));
                    redisTemplate.opsForValue().set("airline:code:" + airlineCode, airline, Duration.ofHours(4));

                    return Optional.of(airline);
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Failed to fetch airline by code: {}", airlineCode, e);
            return getAirlineByCodeFromCache(airlineCode, e);
        }
    }

    // New method: Resolve station by ICAO code
    @CircuitBreaker(name = "reference-manager", fallbackMethod = "getStationByIcaoFromCache")
    public Optional<Map<String, Object>> getStationByIcao(String icaoCode) {
        try {
            String url = referenceManagerBaseUrl + "/api/v1/stations/search?query=" + icaoCode;
            log.debug("Fetching station by ICAO from: {}", url);

            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> stations = response.getBody();

                Optional<Map<String, Object>> station = stations.stream()
                        .filter(s -> icaoCode.equals(s.get("icaoCode")))
                        .findFirst();

                if (station.isPresent()) {
                    Map<String, Object> stationData = station.get();
                    Long stationId = ((Number) stationData.get("id")).longValue();

                    // Cache both by ID and ICAO
                    redisTemplate.opsForValue().set("station:" + stationId, stationData, Duration.ofHours(8));
                    redisTemplate.opsForValue().set("station:icao:" + icaoCode, stationData, Duration.ofHours(8));

                    return Optional.of(stationData);
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Failed to fetch station by ICAO: {}", icaoCode, e);
            return getStationByIcaoFromCache(icaoCode, e);
        }
    }

    // New method: Resolve aircraft by type
    @CircuitBreaker(name = "reference-manager", fallbackMethod = "getAircraftByTypeFromCache")
    public Optional<Map<String, Object>> getAircraftByType(String aircraftType) {
        try {
            String url = referenceManagerBaseUrl + "/api/v1/aircraft?type=" + aircraftType;
            log.debug("Fetching aircraft by type from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> pageResponse = response.getBody();
                List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");

                if (!content.isEmpty()) {
                    Map<String, Object> aircraft = content.get(0);
                    Long aircraftId = ((Number) aircraft.get("id")).longValue();

                    // Cache both by ID and type
                    redisTemplate.opsForValue().set("aircraft:" + aircraftId, aircraft, Duration.ofHours(2));
                    redisTemplate.opsForValue().set("aircraft:type:" + aircraftType, aircraft, Duration.ofHours(2));

                    return Optional.of(aircraft);
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Failed to fetch aircraft by type: {}", aircraftType, e);
            return getAircraftByTypeFromCache(aircraftType, e);
        }
    }

    // Fallback methods
    public Map<String, Object> getAirlineFromCache(Long airlineId, Exception ex) {
        log.warn("Using cached airline data for ID: {} due to: {}", airlineId, ex.getMessage());
        Map<String, Object> cachedData = (Map<String, Object>) redisTemplate.opsForValue().get("airline:" + airlineId);
        return cachedData != null ? cachedData : createFallbackAirline(airlineId);
    }

    public Map<String, Object> getStationFromCache(Long stationId, Exception ex) {
        log.warn("Using cached station data for ID: {} due to: {}", stationId, ex.getMessage());
        Map<String, Object> cachedData = (Map<String, Object>) redisTemplate.opsForValue().get("station:" + stationId);
        return cachedData != null ? cachedData : createFallbackStation(stationId);
    }

    public Map<String, Object> getAircraftFromCache(Long aircraftId, Exception ex) {
        log.warn("Using cached aircraft data for ID: {} due to: {}", aircraftId, ex.getMessage());
        Map<String, Object> cachedData = (Map<String, Object>) redisTemplate.opsForValue().get("aircraft:" + aircraftId);
        return cachedData != null ? cachedData : createFallbackAircraft(aircraftId);
    }

    public Optional<Map<String, Object>> getAirlineByCodeFromCache(String airlineCode, Exception ex) {
        log.warn("Using cached airline data for code: {} due to: {}", airlineCode, ex.getMessage());
        Map<String, Object> cachedData = (Map<String, Object>) redisTemplate.opsForValue().get("airline:code:" + airlineCode);
        return Optional.ofNullable(cachedData);
    }

    public Optional<Map<String, Object>> getStationByIcaoFromCache(String icaoCode, Exception ex) {
        log.warn("Using cached station data for ICAO: {} due to: {}", icaoCode, ex.getMessage());
        Map<String, Object> cachedData = (Map<String, Object>) redisTemplate.opsForValue().get("station:icao:" + icaoCode);
        return Optional.ofNullable(cachedData);
    }

    public Optional<Map<String, Object>> getAircraftByTypeFromCache(String aircraftType, Exception ex) {
        log.warn("Using cached aircraft data for type: {} due to: {}", aircraftType, ex.getMessage());
        Map<String, Object> cachedData = (Map<String, Object>) redisTemplate.opsForValue().get("aircraft:type:" + aircraftType);
        return Optional.ofNullable(cachedData);
    }

    // Fallback data creators
    private Map<String, Object> createFallbackAirline(Long airlineId) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("id", airlineId);
        fallback.put("code", "XX");
        fallback.put("name", "Unknown Airline");
        fallback.put("country", "Unknown");
        fallback.put("isActive", true);
        return fallback;
    }

    private Map<String, Object> createFallbackStation(Long stationId) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("id", stationId);
        fallback.put("icaoCode", "XXXX");
        fallback.put("name", "Unknown Station");
        fallback.put("city", "Unknown");
        fallback.put("country", "Unknown");
        fallback.put("isActive", true);
        return fallback;
    }

    private Map<String, Object> createFallbackAircraft(Long aircraftId) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("id", aircraftId);
        fallback.put("type", "Unknown");
        fallback.put("manufacturer", "Unknown");
        fallback.put("model", "Unknown");
        fallback.put("capacity", 150);
        fallback.put("isActive", true);
        return fallback;
    }

    // Health check method
    public boolean isReferenceManagerHealthy() {
        try {
            String url = referenceManagerBaseUrl + "/actuator/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Reference Manager health check failed", e);
            return false;
        }
    }
}