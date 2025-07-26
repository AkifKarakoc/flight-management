package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightEnrichmentServiceTest {

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private FlightEnrichmentService flightEnrichmentService;

    private OperationalFlight flight;
    private OperationalFlightCreateRequestDto request;
    private Map<String, Object> airlineData;
    private Map<String, Object> stationData;
    private Map<String, Object> aircraftData;

    @BeforeEach
    void setUp() {
        flight = TestDataBuilder.createValidFlight();
        request = TestDataBuilder.createValidFlightRequest();
        airlineData = TestDataBuilder.createAirlineData();
        stationData = TestDataBuilder.createStationData();
        aircraftData = TestDataBuilder.createAircraftData();
    }

    @Test
    void enrichOperationalFlight_AllDataAvailable_Success() {
        // Given
        when(referenceDataService.getAirline(eq(1L))).thenReturn(airlineData);
        when(referenceDataService.getAircraft(eq(1L))).thenReturn(aircraftData);
        when(referenceDataService.getStation(eq(1L))).thenReturn(stationData);
        when(referenceDataService.getStation(eq(2L)))
                .thenReturn(TestDataBuilder.createStationData(2L, "EDDF", "Frankfurt Airport"));

        // When
        flightEnrichmentService.enrichOperationalFlight(flight, request);

        // Then
        assertThat(flight.getAirlineCode()).isEqualTo("TK");
        assertThat(flight.getAirlineName()).isEqualTo("Turkish Airlines");
        assertThat(flight.getAircraftType()).isEqualTo("A320");
        assertThat(flight.getOriginIcaoCode()).isEqualTo("LTBA");
        assertThat(flight.getDestinationIcaoCode()).isEqualTo("EDDF");
    }

    @Test
    void enrichOperationalFlight_AirlineDataMissing_UsesFallback() {
        // Given
        when(referenceDataService.getAirline(eq(1L))).thenReturn(null);
        when(referenceDataService.getAircraft(eq(1L))).thenReturn(aircraftData);
        when(referenceDataService.getStation(eq(1L))).thenReturn(stationData);
        when(referenceDataService.getStation(eq(2L)))
                .thenReturn(TestDataBuilder.createStationData(2L, "EDDF", "Frankfurt Airport"));

        // When
        flightEnrichmentService.enrichOperationalFlight(flight, request);

        // Then
        assertThat(flight.getAirlineCode()).isEqualTo("XX");
        assertThat(flight.getAirlineName()).isEqualTo("Unknown Airline");
        assertThat(flight.getAircraftType()).isEqualTo("A320"); // Other data should still work
    }

    @Test
    void enrichOperationalFlight_ServiceThrowsException_UsesFallback() {
        // Given
        when(referenceDataService.getAirline(eq(1L))).thenThrow(new RuntimeException("Service error"));
        when(referenceDataService.getAircraft(eq(1L))).thenThrow(new RuntimeException("Service error"));
        when(referenceDataService.getStation(eq(1L))).thenThrow(new RuntimeException("Service error"));
        when(referenceDataService.getStation(eq(2L))).thenThrow(new RuntimeException("Service error"));

        // When
        flightEnrichmentService.enrichOperationalFlight(flight, request);

        // Then
        assertThat(flight.getAirlineCode()).isEqualTo("XX");
        assertThat(flight.getAirlineName()).isEqualTo("Unknown Airline");
        assertThat(flight.getAircraftType()).isEqualTo("Unknown");
        assertThat(flight.getOriginIcaoCode()).isEqualTo("XXXX");
        assertThat(flight.getDestinationIcaoCode()).isEqualTo("YYYY");
    }

    @Test
    void enrichFromCsvData_AllDataResolved_Success() {
        // Given
        when(referenceDataService.getAirlineByCode("TK")).thenReturn(Optional.of(airlineData));
        when(referenceDataService.getAircraftByType("A320")).thenReturn(Optional.of(aircraftData));
        when(referenceDataService.getStationByIcao("LTBA")).thenReturn(Optional.of(stationData));
        when(referenceDataService.getStationByIcao("EDDF"))
                .thenReturn(Optional.of(TestDataBuilder.createStationData(2L, "EDDF", "Frankfurt Airport")));

        // When
        flightEnrichmentService.enrichFromCsvData(flight, "TK", "A320", "LTBA", "EDDF");

        // Then
        assertThat(flight.getAirlineId()).isEqualTo(1L);
        assertThat(flight.getAirlineCode()).isEqualTo("TK");
        assertThat(flight.getAirlineName()).isEqualTo("Turkish Airlines");
        assertThat(flight.getAircraftId()).isEqualTo(1L);
        assertThat(flight.getAircraftType()).isEqualTo("A320");
        assertThat(flight.getOriginStationId()).isEqualTo(1L);
        assertThat(flight.getOriginIcaoCode()).isEqualTo("LTBA");
        assertThat(flight.getDestinationStationId()).isEqualTo(2L);
        assertThat(flight.getDestinationIcaoCode()).isEqualTo("EDDF");
    }

    @Test
    void enrichFromCsvData_AirlineNotFound_KeepsOriginalCode() {
        // Given
        when(referenceDataService.getAirlineByCode("XX")).thenReturn(Optional.empty());
        when(referenceDataService.getAircraftByType("A320")).thenReturn(Optional.of(aircraftData));
        when(referenceDataService.getStationByIcao("LTBA")).thenReturn(Optional.of(stationData));
        when(referenceDataService.getStationByIcao("EDDF"))
                .thenReturn(Optional.of(TestDataBuilder.createStationData(2L, "EDDF", "Frankfurt Airport")));

        // When
        flightEnrichmentService.enrichFromCsvData(flight, "XX", "A320", "LTBA", "EDDF");

        // Then
        assertThat(flight.getAirlineCode()).isEqualTo("XX");
        assertThat(flight.getAirlineName()).isEqualTo("Unknown Airline");
        assertThat(flight.getAircraftType()).isEqualTo("A320"); // Other data should still work
    }

    @Test
    void enrichFromCsvData_AircraftNotFound_KeepsOriginalType() {
        // Given
        when(referenceDataService.getAirlineByCode("TK")).thenReturn(Optional.of(airlineData));
        when(referenceDataService.getAircraftByType("UNKNOWN")).thenReturn(Optional.empty());
        when(referenceDataService.getStationByIcao("LTBA")).thenReturn(Optional.of(stationData));
        when(referenceDataService.getStationByIcao("EDDF"))
                .thenReturn(Optional.of(TestDataBuilder.createStationData(2L, "EDDF", "Frankfurt Airport")));

        // When
        flightEnrichmentService.enrichFromCsvData(flight, "TK", "UNKNOWN", "LTBA", "EDDF");

        // Then
        assertThat(flight.getAirlineCode()).isEqualTo("TK");
        assertThat(flight.getAirlineName()).isEqualTo("Turkish Airlines");
        assertThat(flight.getAircraftType()).isEqualTo("UNKNOWN");
    }

    @Test
    void enrichFromCsvData_StationsNotFound_KeepsOriginalCodes() {
        // Given
        when(referenceDataService.getAirlineByCode("TK")).thenReturn(Optional.of(airlineData));
        when(referenceDataService.getAircraftByType("A320")).thenReturn(Optional.of(aircraftData));
        when(referenceDataService.getStationByIcao("XXXX")).thenReturn(Optional.empty());
        when(referenceDataService.getStationByIcao("YYYY")).thenReturn(Optional.empty());

        // When
        flightEnrichmentService.enrichFromCsvData(flight, "TK", "A320", "XXXX", "YYYY");

        // Then
        assertThat(flight.getAirlineCode()).isEqualTo("TK");
        assertThat(flight.getAirlineName()).isEqualTo("Turkish Airlines");
        assertThat(flight.getAircraftType()).isEqualTo("A320");
        assertThat(flight.getOriginIcaoCode()).isEqualTo("XXXX");
        assertThat(flight.getDestinationIcaoCode()).isEqualTo("YYYY");
    }

    @Test
    void enrichFromCsvData_ServiceThrowsException_KeepsOriginalValues() {
        // Given
        when(referenceDataService.getAirlineByCode("TK")).thenThrow(new RuntimeException("Service error"));
        when(referenceDataService.getAircraftByType("A320")).thenThrow(new RuntimeException("Service error"));
        when(referenceDataService.getStationByIcao("LTBA")).thenThrow(new RuntimeException("Service error"));
        when(referenceDataService.getStationByIcao("EDDF")).thenThrow(new RuntimeException("Service error"));

        // When
        flightEnrichmentService.enrichFromCsvData(flight, "TK", "A320", "LTBA", "EDDF");

        // Then
        assertThat(flight.getAirlineCode()).isEqualTo("TK");
        assertThat(flight.getAircraftType()).isEqualTo("A320");
        assertThat(flight.getOriginIcaoCode()).isEqualTo("LTBA");
        assertThat(flight.getDestinationIcaoCode()).isEqualTo("EDDF");
    }

    @Test
    void enrichFromCsvData_PartialDataAvailable_MixedResults() {
        // Given
        when(referenceDataService.getAirlineByCode("TK")).thenReturn(Optional.of(airlineData));
        when(referenceDataService.getAircraftByType("UNKNOWN")).thenReturn(Optional.empty());
        when(referenceDataService.getStationByIcao("LTBA")).thenReturn(Optional.of(stationData));
        when(referenceDataService.getStationByIcao("XXXX")).thenReturn(Optional.empty());

        // When
        flightEnrichmentService.enrichFromCsvData(flight, "TK", "UNKNOWN", "LTBA", "XXXX");

        // Then
        // Successfully resolved data
        assertThat(flight.getAirlineId()).isEqualTo(1L);
        assertThat(flight.getAirlineCode()).isEqualTo("TK");
        assertThat(flight.getAirlineName()).isEqualTo("Turkish Airlines");
        assertThat(flight.getOriginStationId()).isEqualTo(1L);
        assertThat(flight.getOriginIcaoCode()).isEqualTo("LTBA");

        // Unresolved data - kept as original
        assertThat(flight.getAircraftType()).isEqualTo("UNKNOWN");
        assertThat(flight.getDestinationIcaoCode()).isEqualTo("XXXX");
    }
}