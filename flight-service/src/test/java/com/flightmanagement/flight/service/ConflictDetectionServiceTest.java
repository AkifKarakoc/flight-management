package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.ConflictType;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.util.TestDataBuilder;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConflictDetectionServiceTest {

    @Mock
    private OperationalFlightRepository flightRepository;

    @InjectMocks
    private ConflictDetectionService conflictDetectionService;

    private OperationalFlightCreateRequestDto flightRequest;
    private OperationalFlight existingFlight;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now().plusDays(1);
        flightRequest = TestDataBuilder.createValidFlightRequest();
        flightRequest.setFlightDate(testDate);

        existingFlight = TestDataBuilder.createValidFlight();
        existingFlight.setFlightDate(testDate);
    }

    @Test
    void detectConflicts_NoConflicts_ReturnsEmptyList() {
        // Given
        when(flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(any(), any(), any()))
                .thenReturn(false);
        when(flightRepository.findByAircraftIdAndFlightDate(any(), any()))
                .thenReturn(Collections.emptyList());
        when(flightRepository.findByOriginStationIdAndFlightDate(any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<ConflictDetectionService.Conflict> conflicts = conflictDetectionService.detectConflicts(flightRequest);

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    void detectConflicts_FlightNumberDuplicate_ReturnsConflict() {
        // Given
        when(flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(
                eq(flightRequest.getFlightNumber()),
                eq(flightRequest.getAirlineId()),
                eq(flightRequest.getFlightDate())))
                .thenReturn(true);
        when(flightRepository.findByAircraftIdAndFlightDate(any(), any()))
                .thenReturn(Collections.emptyList());
        when(flightRepository.findByOriginStationIdAndFlightDate(any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<ConflictDetectionService.Conflict> conflicts = conflictDetectionService.detectConflicts(flightRequest);

        // Then
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getType()).isEqualTo(ConflictType.FLIGHT_NUMBER_DUPLICATE);
        assertThat(conflicts.get(0).getDescription())
                .contains("Flight number already exists for this airline on this date");
    }

    @Test
    void detectConflicts_AircraftDoubleBooking_ReturnsConflict() {
        // Given
        existingFlight.setAircraftId(flightRequest.getAircraftId());
        existingFlight.setScheduledDepartureTime(LocalTime.of(10, 0)); // Overlapping time
        existingFlight.setScheduledArrivalTime(LocalTime.of(12, 0));

        when(flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(any(), any(), any()))
                .thenReturn(false);
        when(flightRepository.findByAircraftIdAndFlightDate(
                eq(flightRequest.getAircraftId()),
                eq(flightRequest.getFlightDate())))
                .thenReturn(List.of(existingFlight));
        when(flightRepository.findByOriginStationIdAndFlightDate(any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<ConflictDetectionService.Conflict> conflicts = conflictDetectionService.detectConflicts(flightRequest);

        // Then
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getType()).isEqualTo(ConflictType.AIRCRAFT_DOUBLE_BOOKING);
        assertThat(conflicts.get(0).getDescription())
                .contains("Aircraft already booked for flight");
    }

    @Test
    void detectConflicts_AircraftNoTimeOverlap_NoConflict() {
        // Given
        existingFlight.setAircraftId(flightRequest.getAircraftId());
        existingFlight.setScheduledDepartureTime(LocalTime.of(8, 0)); // No overlap
        existingFlight.setScheduledArrivalTime(LocalTime.of(9, 0));   // Ends before new flight

        when(flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(any(), any(), any()))
                .thenReturn(false);
        when(flightRepository.findByAircraftIdAndFlightDate(
                eq(flightRequest.getAircraftId()),
                eq(flightRequest.getFlightDate())))
                .thenReturn(List.of(existingFlight));
        when(flightRepository.findByOriginStationIdAndFlightDate(any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<ConflictDetectionService.Conflict> conflicts = conflictDetectionService.detectConflicts(flightRequest);

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    void detectConflicts_SlotConflict_ReturnsConflict() {
        // Given
        existingFlight.setOriginStationId(flightRequest.getOriginStationId());
        existingFlight.setScheduledDepartureTime(LocalTime.of(10, 45)); // Within 30-minute slot window

        when(flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(any(), any(), any()))
                .thenReturn(false);
        when(flightRepository.findByAircraftIdAndFlightDate(any(), any()))
                .thenReturn(Collections.emptyList());
        when(flightRepository.findByOriginStationIdAndFlightDate(
                eq(flightRequest.getOriginStationId()),
                eq(flightRequest.getFlightDate())))
                .thenReturn(List.of(existingFlight));

        // When
        List<ConflictDetectionService.Conflict> conflicts = conflictDetectionService.detectConflicts(flightRequest);

        // Then
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getType()).isEqualTo(ConflictType.SLOT_CONFLICT);
        assertThat(conflicts.get(0).getDescription())
                .contains("Departure slot conflict with flight");
    }

    @Test
    void detectConflicts_SlotNoConflict_NoConflict() {
        // Given
        existingFlight.setOriginStationId(flightRequest.getOriginStationId());
        existingFlight.setScheduledDepartureTime(LocalTime.of(11, 30)); // Outside 30-minute window

        when(flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(any(), any(), any()))
                .thenReturn(false);
        when(flightRepository.findByAircraftIdAndFlightDate(any(), any()))
                .thenReturn(Collections.emptyList());
        when(flightRepository.findByOriginStationIdAndFlightDate(
                eq(flightRequest.getOriginStationId()),
                eq(flightRequest.getFlightDate())))
                .thenReturn(List.of(existingFlight));

        // When
        List<ConflictDetectionService.Conflict> conflicts = conflictDetectionService.detectConflicts(flightRequest);

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    void detectConflicts_MultipleConflicts_ReturnsAllConflicts() {
        // Given
        // Setup flight number duplicate
        when(flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(any(), any(), any()))
                .thenReturn(true);

        // Setup aircraft double booking
        existingFlight.setAircraftId(flightRequest.getAircraftId());
        existingFlight.setScheduledDepartureTime(LocalTime.of(10, 0));
        existingFlight.setScheduledArrivalTime(LocalTime.of(12, 0));
        when(flightRepository.findByAircraftIdAndFlightDate(any(), any()))
                .thenReturn(List.of(existingFlight));

        when(flightRepository.findByOriginStationIdAndFlightDate(any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<ConflictDetectionService.Conflict> conflicts = conflictDetectionService.detectConflicts(flightRequest);

        // Then
        assertThat(conflicts).hasSize(2);
        assertThat(conflicts).extracting(ConflictDetectionService.Conflict::getType)
                .containsExactlyInAnyOrder(
                        ConflictType.FLIGHT_NUMBER_DUPLICATE,
                        ConflictType.AIRCRAFT_DOUBLE_BOOKING
                );
    }
}