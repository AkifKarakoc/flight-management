package com.flightmanagement.flight.config;

import com.flightmanagement.flight.service.ReferenceDataService;
import com.flightmanagement.flight.util.TestDataBuilder;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@TestConfiguration
public class FlightTestConfiguration {

    @Bean
    @Primary
    public ReferenceDataService mockReferenceDataService() {
        ReferenceDataService mockService = Mockito.mock(ReferenceDataService.class);

        // Mock airline data
        when(mockService.getAirline(1L)).thenReturn(TestDataBuilder.createAirlineData());
        when(mockService.getAirline(2L)).thenReturn(TestDataBuilder.createAirlineData(2L, "LH", "Lufthansa"));
        when(mockService.getAirlineByCode("TK")).thenReturn(Optional.of(TestDataBuilder.createAirlineData()));
        when(mockService.getAirlineByCode("LH")).thenReturn(Optional.of(TestDataBuilder.createAirlineData(2L, "LH", "Lufthansa")));
        when(mockService.getAirlineByCode("XX")).thenReturn(Optional.empty());

        // Mock station data
        when(mockService.getStation(1L)).thenReturn(TestDataBuilder.createStationData());
        when(mockService.getStation(2L)).thenReturn(TestDataBuilder.createStationData(2L, "EDDF", "Frankfurt Airport"));
        when(mockService.getStationByIcao("LTBA")).thenReturn(Optional.of(TestDataBuilder.createStationData()));
        when(mockService.getStationByIcao("EDDF")).thenReturn(Optional.of(TestDataBuilder.createStationData(2L, "EDDF", "Frankfurt Airport")));
        when(mockService.getStationByIcao("KJFK")).thenReturn(Optional.of(TestDataBuilder.createStationData(3L, "KJFK", "JFK Airport")));
        when(mockService.getStationByIcao("XXXX")).thenReturn(Optional.empty());

        // Mock aircraft data
        when(mockService.getAircraft(1L)).thenReturn(TestDataBuilder.createAircraftData());
        when(mockService.getAircraft(2L)).thenReturn(TestDataBuilder.createAircraftData(2L, "B737", "Boeing"));
        when(mockService.getAircraftByType("A320")).thenReturn(Optional.of(TestDataBuilder.createAircraftData()));
        when(mockService.getAircraftByType("B737")).thenReturn(Optional.of(TestDataBuilder.createAircraftData(2L, "B737", "Boeing")));
        when(mockService.getAircraftByType("Unknown")).thenReturn(Optional.empty());

        // Mock health check
        when(mockService.isReferenceManagerHealthy()).thenReturn(true);

        return mockService;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, Object> mockKafkaTemplate() {
        KafkaTemplate<String, Object> mockTemplate = Mockito.mock(KafkaTemplate.class);

        // Mock successful send operations
        when(mockTemplate.send(anyString(), anyString(), any())).thenReturn(null);
        when(mockTemplate.send(anyString(), any())).thenReturn(null);

        return mockTemplate;
    }

    @Bean
    @Primary
    public SimpMessagingTemplate mockSimpMessagingTemplate() {
        SimpMessagingTemplate mockTemplate = Mockito.mock(SimpMessagingTemplate.class);

        // Mock WebSocket message sending - daha spesifik olalım
        Mockito.doNothing().when(mockTemplate).convertAndSend(anyString(), any(Object.class));

        return mockTemplate;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RestTemplate mockRestTemplate() {
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);

        // Mock successful health check
        when(mockRestTemplate.getForEntity(contains("/actuator/health"), eq(Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(Map.of("status", "UP")));

        return mockRestTemplate;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> mockRedisTemplate() {
        RedisTemplate<String, Object> mockTemplate = Mockito.mock(RedisTemplate.class);
        RedisConnectionFactory mockConnectionFactory = Mockito.mock(RedisConnectionFactory.class);

        when(mockTemplate.getConnectionFactory()).thenReturn(mockConnectionFactory);

        // Mock basic Redis operations
        when(mockTemplate.opsForValue()).thenReturn(Mockito.mock(org.springframework.data.redis.core.ValueOperations.class));
        Mockito.doNothing().when(mockTemplate).delete(anyString());

        return mockTemplate;
    }
}