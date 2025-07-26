package com.flightmanagement.flight.service;

import com.flightmanagement.flight.config.FlightServiceProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final FlightServiceProperties properties;

    @CircuitBreaker(name = "reference-manager", fallbackMethod = "getAirlineFromCache")
    @Cacheable(value = "airlines", key = "#airlineId")
    public Map<String, Object> getAirline(Long airlineId) {
        try {
            String url = properties.getReferenceManager().getBaseUrl() + "/api/v1/airlines/" + airlineId;
            log.debug("Fetching airline data from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> airline = response.getBody();

                // Cache with configured TTL
                redisTemplate.opsForValue().set("airline:" + airlineId, airline,
                        properties.getRedis().getTtl().getAirlines());
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
            String url = properties.getReferenceManager().getBaseUrl() + "/api/v1/stations/" + stationId;
            log.debug("Fetching station data from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> station = response.getBody();

                redisTemplate.opsForValue().set("station:" + stationId, station,
                        properties.getRedis().getTtl().getStations());
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
            String url = properties.getReferenceManager().getBaseUrl() + "/api/v1/aircraft/" + aircraftId;
            log.debug("Fetching aircraft data from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> aircraft = response.getBody();

                redisTemplate.opsForValue().set("aircraft:" + aircraftId, aircraft,
                        properties.getRedis().getTtl().getAircraft());
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
            String url = properties.getReferenceManager().getBaseUrl() + "/api/v1/airlines?code=" + airlineCode;
            log.debug("Fetching airline by code from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> pageResponse = response.getBody();
                List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");

                if (!content.isEmpty()) {
                    Map<String, Object> airline = content.get(0);
                    Long airlineId = ((Number) airline.get("id")).longValue();

                    // Cache both by ID and code
                    redisTemplate.opsForValue().set("airline:" + airlineId, airline,
                            properties.getRedis().getTtl().getAirlines());
                    redisTemplate.opsForValue().set("airline:code:" + airlineCode, airline,
                            properties.getRedis().getTtl().getAirlines());

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
            String url = properties.getReferenceManager().getBaseUrl() + "/api/v1/stations/search?query=" + icaoCode;
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
                    redisTemplate.opsForValue().set("station:" + stationId, stationData,
                            properties.getRedis().getTtl().getStations());
                    redisTemplate.opsForValue().set("station:icao:" + icaoCode, stationData,
                            properties.getRedis().getTtl().getStations());

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
            String url = properties.getReferenceManager().getBaseUrl() + "/api/v1/aircraft?type=" + aircraftType;
            log.debug("Fetching aircraft by type from: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> pageResponse = response.getBody();
                List<Map<String, Object>> content = (List<Map<String, Object>>) pageResponse.get("content");

                if (!content.isEmpty()) {
                    Map<String, Object> aircraft = content.get(0);
                    Long aircraftId = ((Number) aircraft.get("id")).longValue();

                    // Cache both by ID and type
                    redisTemplate.opsForValue().set("aircraft:" + aircraftId, aircraft,
                            properties.getRedis().getTtl().getAircraft());
                    redisTemplate.opsForValue().set("aircraft:type:" + aircraftType, aircraft,
                            properties.getRedis().getTtl().getAircraft());

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
            String url = properties.getReferenceManager().getBaseUrl() + "/actuator/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Reference Manager health check failed", e);
            return false;
        }
    }

    // Cache invalidation methods
    public void invalidateAirlineCache(Long airlineId) {
        redisTemplate.delete("airline:" + airlineId);
        log.debug("Invalidated airline cache for ID: {}", airlineId);
    }

    public void invalidateStationCache(Long stationId) {
        redisTemplate.delete("station:" + stationId);
        log.debug("Invalidated station cache for ID: {}", stationId);
    }

    public void invalidateAircraftCache(Long aircraftId) {
        redisTemplate.delete("aircraft:" + aircraftId);
        log.debug("Invalidated aircraft cache for ID: {}", aircraftId);
    }

    public void invalidateAllCaches() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.info("Invalidated all reference data caches");
    }

    // Batch operations for performance
    public Map<Long, Map<String, Object>> getAirlinesBatch(List<Long> airlineIds) {
        Map<Long, Map<String, Object>> result = new HashMap<>();

        for (Long airlineId : airlineIds) {
            try {
                Map<String, Object> airline = getAirline(airlineId);
                if (airline != null) {
                    result.put(airlineId, airline);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch airline in batch for ID: {}", airlineId, e);
                result.put(airlineId, createFallbackAirline(airlineId));
            }
        }

        return result;
    }

    public Map<Long, Map<String, Object>> getStationsBatch(List<Long> stationIds) {
        Map<Long, Map<String, Object>> result = new HashMap<>();

        for (Long stationId : stationIds) {
            try {
                Map<String, Object> station = getStation(stationId);
                if (station != null) {
                    result.put(stationId, station);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch station in batch for ID: {}", stationId, e);
                result.put(stationId, createFallbackStation(stationId));
            }
        }

        return result;
    }

    public Map<Long, Map<String, Object>> getAircraftBatch(List<Long> aircraftIds) {
        Map<Long, Map<String, Object>> result = new HashMap<>();

        for (Long aircraftId : aircraftIds) {
            try {
                Map<String, Object> aircraft = getAircraft(aircraftId);
                if (aircraft != null) {
                    result.put(aircraftId, aircraft);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch aircraft in batch for ID: {}", aircraftId, e);
                result.put(aircraftId, createFallbackAircraft(aircraftId));
            }
        }

        return result;
    }
}