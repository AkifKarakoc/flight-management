package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.FlightType;
import com.flightmanagement.flight.exception.FlightConflictException;
import com.flightmanagement.flight.mapper.OperationalFlightMapper;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationalFlightServiceTest {

    @Mock
    private OperationalFlightRepository flightRepository;

    @Mock
    private OperationalFlightMapper flightMapper;

    @Mock
    private ConflictDetectionService conflictService;

    @Mock
    private ReferenceDataService referenceService;

    @Mock
    private EventPublishService eventPublishService;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private FlightVersionService versionService;

    @InjectMocks
    private OperationalFlightService flightService;

    private OperationalFlightCreateRequestDto flightRequest;
    private UserContext adminContext;
    private OperationalFlight flight;

    @BeforeEach
    void setUp() {
        flightRequest = new OperationalFlightCreateRequestDto();
        flightRequest.setFlightNumber("TK123");
        flightRequest.setAirlineId(1L);
        flightRequest.setAircraftId(1L);
        flightRequest.setFlightDate(LocalDate.now().plusDays(1));
        flightRequest.setScheduledDepartureTime(LocalTime.of(10, 30));
        flightRequest.setScheduledArrivalTime(LocalTime.of(13, 45));
        flightRequest.setOriginStationId(1L);
        flightRequest.setDestinationStationId(2L);
        flightRequest.setFlightType(FlightType.PASSENGER);

        adminContext = UserContext.builder()
                .username("admin")
                .roles(List.of("ROLE_ADMIN"))
                .build();

        flight = OperationalFlight.builder()
                .id(1L)
                .flightNumber("TK123")
                .airlineId(1L)
                .build();
    }

    @Test
    void createFlight_Success() {
        // Given
        when(conflictService.detectConflicts(any())).thenReturn(Collections.emptyList());
        when(flightMapper.toEntity(any())).thenReturn(flight);
        when(flightRepository.save(any())).thenReturn(flight);
        when(flightMapper.toResponseDto(any())).thenReturn(null);

        // When
        flightService.createFlight(flightRequest, adminContext);

        // Then
        verify(flightRepository).save(any(OperationalFlight.class));
        verify(versionService).createInitialVersion(any());
        verify(eventPublishService).publishFlightEvent(eq("FLIGHT_CREATED"), any(), any());
        verify(webSocketService).notifyFlightCreated(any());
    }

    @Test
    void createFlight_ConflictDetected_ThrowsException() {
        // Given
        ConflictDetectionService.Conflict conflict = new ConflictDetectionService.Conflict(
                com.flightmanagement.flight.enums.ConflictType.AIRCRAFT_DOUBLE_BOOKING, "Test conflict");
        when(conflictService.detectConflicts(any())).thenReturn(List.of(conflict));

        // When & Then
        assertThatThrownBy(() -> flightService.createFlight(flightRequest, adminContext))
                .isInstanceOf(FlightConflictException.class)
                .hasMessageContaining("Conflicts detected");
    }
}