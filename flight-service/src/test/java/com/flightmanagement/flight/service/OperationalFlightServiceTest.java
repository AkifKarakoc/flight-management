package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.dto.response.PagedResponse;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.FlightType;
import com.flightmanagement.flight.exception.FlightConflictException;
import com.flightmanagement.flight.exception.FlightNotFoundException;
import com.flightmanagement.flight.exception.UnauthorizedFlightAccessException;
import com.flightmanagement.flight.mapper.OperationalFlightMapperImpl;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.security.UserContext;
import com.flightmanagement.flight.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationalFlightServiceTest {

    @Mock
    private OperationalFlightRepository flightRepository;

    @Mock
    private OperationalFlightMapperImpl flightMapper;

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
    private UserContext airlineUserContext;
    private OperationalFlight flight;
    private OperationalFlightResponseDto flightResponse;

    @BeforeEach
    void setUp() {
        flightRequest = TestDataBuilder.createValidFlightRequest();
        adminContext = TestDataBuilder.createAdminUserContext();
        airlineUserContext = TestDataBuilder.createAirlineUserContext();
        flight = TestDataBuilder.createValidFlight();
        flightResponse = new OperationalFlightResponseDto();
        flightResponse.setId(1L);
        flightResponse.setFlightNumber("TK123");
    }

    @Test
    void createFlight_ValidRequest_Success() {
        // Given
        when(conflictService.detectConflicts(any())).thenReturn(Collections.emptyList());
        when(flightMapper.toEntity(any())).thenReturn(flight);
        when(flightRepository.save(any())).thenReturn(flight);
        when(flightMapper.toResponseDto(any())).thenReturn(flightResponse);

        // When
        OperationalFlightResponseDto result = flightService.createFlight(flightRequest, adminContext);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFlightNumber()).isEqualTo("TK123");

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

        verify(flightRepository, never()).save(any());
        verify(eventPublishService, never()).publishFlightEvent(any(), any(), any());
    }

    @Test
    void createFlight_AirlineUserDifferentAirline_ThrowsException() {
        // Given
        flightRequest.setAirlineId(999L); // Different airline

        // When & Then
        assertThatThrownBy(() -> flightService.createFlight(flightRequest, airlineUserContext))
                .isInstanceOf(UnauthorizedFlightAccessException.class)
                .hasMessageContaining("Cannot create flight for different airline");
    }

    @Test
    void getFlights_AdminUser_ReturnsAllFlights() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<OperationalFlight> flightPage = new PageImpl<>(List.of(flight));
        when(flightRepository.findAll(pageable)).thenReturn(flightPage);
        when(flightMapper.toResponseDto(any())).thenReturn(flightResponse);

        // When
        PagedResponse<OperationalFlightResponseDto> result = flightService.getFlights(pageable, adminContext);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(flightRepository).findAll(pageable);
        verify(flightRepository, never()).findByAirlineId(any(), any());
    }

    @Test
    void getFlights_AirlineUser_ReturnsOnlyOwnFlights() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<OperationalFlight> flightPage = new PageImpl<>(List.of(flight));
        when(flightRepository.findByAirlineId(eq(1L), eq(pageable))).thenReturn(flightPage);
        when(flightMapper.toResponseDto(any())).thenReturn(flightResponse);

        // When
        PagedResponse<OperationalFlightResponseDto> result = flightService.getFlights(pageable, airlineUserContext);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(flightRepository).findByAirlineId(eq(1L), eq(pageable));
        verify(flightRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getFlightById_ExistingFlight_AdminUser_Success() {
        // Given
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightMapper.toResponseDto(flight)).thenReturn(flightResponse);

        // When
        OperationalFlightResponseDto result = flightService.getFlightById(1L, adminContext);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(flightRepository).findById(1L);
    }

    @Test
    void getFlightById_NonExistingFlight_ThrowsException() {
        // Given
        when(flightRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> flightService.getFlightById(999L, adminContext))
                .isInstanceOf(FlightNotFoundException.class)
                .hasMessageContaining("Flight not found with id: 999");
    }

    @Test
    void getFlightById_AirlineUserDifferentAirline_ThrowsException() {
        // Given
        flight.setAirlineId(999L); // Different airline
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        // When & Then
        assertThatThrownBy(() -> flightService.getFlightById(1L, airlineUserContext))
                .isInstanceOf(UnauthorizedFlightAccessException.class)
                .hasMessageContaining("Cannot access flights from different airline");
    }

    @Test
    void updateFlight_ValidRequest_Success() {
        // Given
        OperationalFlight existingFlight = TestDataBuilder.createValidFlight();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(existingFlight));
        when(versionService.isMajorChange(any(), any())).thenReturn(true);
        when(flightRepository.save(any())).thenReturn(existingFlight);
        when(flightMapper.toResponseDto(any())).thenReturn(flightResponse);

        // When
        OperationalFlightResponseDto result = flightService.updateFlight(1L, flightRequest, adminContext);

        // Then
        assertThat(result).isNotNull();

        verify(flightMapper).updateEntityFromDto(eq(flightRequest), any());
        verify(flightRepository).save(any());
        verify(versionService).createVersionEntry(any(), any(), any(), eq(true));
        verify(eventPublishService).publishFlightEvent(eq("FLIGHT_UPDATED"), any(), any());
        verify(webSocketService).notifyFlightUpdated(any(), any());
    }

    @Test
    void updateFlight_MinorChange_VersionNotIncremented() {
        // Given
        OperationalFlight existingFlight = TestDataBuilder.createValidFlight();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(existingFlight));
        when(versionService.isMajorChange(any(), any())).thenReturn(false);
        when(flightRepository.save(any())).thenReturn(existingFlight);
        when(flightMapper.toResponseDto(any())).thenReturn(flightResponse);

        Integer originalVersion = existingFlight.getVersion();

        // When
        flightService.updateFlight(1L, flightRequest, adminContext);

        // Then
        verify(versionService).createVersionEntry(any(), any(), any(), eq(false));
        // Version should remain the same for minor changes
    }

    @Test
    void deleteFlight_ExistingFlight_Success() {
        // Given
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any())).thenReturn(flight);

        // When
        flightService.deleteFlight(1L, adminContext);

        // Then
        verify(flightRepository).save(any());
        verify(eventPublishService).publishFlightEvent(eq("FLIGHT_DELETED"), any(), any());

        // Verify soft delete
        assertThat(flight.getIsActive()).isFalse();
        assertThat(flight.getUpdatedBy()).isEqualTo(adminContext.getUsername());
    }

    @Test
    void deleteFlight_AirlineUserDifferentAirline_ThrowsException() {
        // Given
        flight.setAirlineId(999L); // Different airline
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        // When & Then
        assertThatThrownBy(() -> flightService.deleteFlight(1L, airlineUserContext))
                .isInstanceOf(UnauthorizedFlightAccessException.class);

        verify(flightRepository, never()).save(any());
        verify(eventPublishService, never()).publishFlightEvent(any(), any(), any());
    }
}