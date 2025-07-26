package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.request.FlightCreateRequestDto;
import com.flightmanagement.reference.entity.Flight;
import com.flightmanagement.reference.enums.FlightType;
import com.flightmanagement.reference.exception.DuplicateReferenceException;
import com.flightmanagement.reference.mapper.FlightMapper;
import com.flightmanagement.reference.repository.FlightRepository;
import com.flightmanagement.reference.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private FlightMapper flightMapper;

    @Mock
    private EventPublishService eventPublishService;

    @Mock
    private RouteSegmentService routeSegmentService;

    @InjectMocks
    private FlightService flightService;

    private FlightCreateRequestDto flightRequest;
    private UserContext adminContext;

    @BeforeEach
    void setUp() {
        flightRequest = new FlightCreateRequestDto();
        flightRequest.setFlightNumber("TK123");
        flightRequest.setAirlineId(1L);
        flightRequest.setAircraftId(1L);
        flightRequest.setFlightDate(LocalDate.now().plusDays(1));
        flightRequest.setDepartureTime(LocalTime.of(10, 30));
        flightRequest.setArrivalTime(LocalTime.of(13, 45));
        flightRequest.setFlightType(FlightType.PASSENGER);
        flightRequest.setSegments(Collections.emptyList());

        adminContext = UserContext.builder()
                .username("admin")
                .roles(Collections.singletonList("ROLE_ADMIN"))
                .build();
    }

    @Test
    void createFlight_Success() {
        // Given
        when(flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(
                anyString(), any(), any())).thenReturn(false);
        when(flightMapper.toEntity(any())).thenReturn(new Flight());
        when(flightRepository.save(any())).thenReturn(new Flight());
        when(flightMapper.toResponseDto(any())).thenReturn(null);

        // When
        flightService.createFlight(flightRequest, adminContext);

        // Then
        verify(flightRepository).save(any(Flight.class));
        verify(eventPublishService).publishReferenceEvent("CREATED", "FLIGHT", any(), any());
    }

    @Test
    void createFlight_DuplicateFlight_ThrowsException() {
        // Given
        when(flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(
                anyString(), any(), any())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> flightService.createFlight(flightRequest, adminContext))
                .isInstanceOf(DuplicateReferenceException.class)
                .hasMessageContaining("Flight already exists");
    }
}