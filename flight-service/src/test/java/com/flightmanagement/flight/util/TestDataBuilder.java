package com.flightmanagement.flight.util;

import com.flightmanagement.flight.dto.request.FlightStatusUpdateRequestDto;
import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.entity.FlightUploadBatch;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.FlightStatus;
import com.flightmanagement.flight.enums.FlightType;
import com.flightmanagement.flight.enums.UploadStatus;
import com.flightmanagement.flight.security.UserContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDataBuilder {

    // User Context Builders
    public static UserContext createAdminUserContext() {
        return UserContext.builder()
                .userId(1L)
                .username("admin")
                .airlineId(null)
                .roles(List.of("ROLE_ADMIN"))
                .build();
    }

    public static UserContext createAirlineUserContext() {
        return UserContext.builder()
                .userId(2L)
                .username("airline_user")
                .airlineId(1L)
                .roles(List.of("ROLE_AIRLINE_USER"))
                .build();
    }

    public static UserContext createAirlineUserContext(Long airlineId) {
        return UserContext.builder()
                .userId(2L)
                .username("airline_user")
                .airlineId(airlineId)
                .roles(List.of("ROLE_AIRLINE_USER"))
                .build();
    }

    // Flight Request Builders
    public static OperationalFlightCreateRequestDto createValidFlightRequest() {
        OperationalFlightCreateRequestDto request = new OperationalFlightCreateRequestDto();
        request.setFlightNumber("TK123");
        request.setAirlineId(1L);
        request.setAircraftId(1L);
        request.setFlightDate(LocalDate.now().plusDays(1));
        request.setScheduledDepartureTime(LocalTime.of(10, 30));
        request.setScheduledArrivalTime(LocalTime.of(13, 45));
        request.setOriginStationId(1L);
        request.setDestinationStationId(2L);
        request.setFlightType(FlightType.PASSENGER);
        request.setGate("A12");
        request.setTerminal("1");
        return request;
    }

    public static OperationalFlightCreateRequestDto createFlightRequest(String flightNumber, Long airlineId) {
        OperationalFlightCreateRequestDto request = createValidFlightRequest();
        request.setFlightNumber(flightNumber);
        request.setAirlineId(airlineId);
        return request;
    }

    public static OperationalFlightCreateRequestDto createFlightRequestWithDate(LocalDate flightDate) {
        OperationalFlightCreateRequestDto request = createValidFlightRequest();
        request.setFlightDate(flightDate);
        return request;
    }

    // Flight Entity Builders
    public static OperationalFlight createValidFlight() {
        return OperationalFlight.builder()
                .id(1L)
                .flightNumber("TK123")
                .airlineId(1L)
                .airlineCode("TK")
                .airlineName("Turkish Airlines")
                .aircraftId(1L)
                .aircraftType("A320")
                .flightDate(LocalDate.now().plusDays(1))
                .scheduledDepartureTime(LocalTime.of(10, 30))
                .scheduledArrivalTime(LocalTime.of(13, 45))
                .originStationId(1L)
                .originIcaoCode("LTBA")
                .destinationStationId(2L)
                .destinationIcaoCode("EDDF")
                .gate("A12")
                .terminal("1")
                .status(FlightStatus.SCHEDULED)
                .flightType(FlightType.PASSENGER)
                .version(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy("testuser")
                .updatedBy("testuser")
                .build();
    }

    public static OperationalFlight createFlight(Long id, String flightNumber, Long airlineId) {
        OperationalFlight flight = createValidFlight();
        flight.setId(id);
        flight.setFlightNumber(flightNumber);
        flight.setAirlineId(airlineId);
        return flight;
    }

    public static OperationalFlight createFlightWithStatus(FlightStatus status) {
        OperationalFlight flight = createValidFlight();
        flight.setStatus(status);
        return flight;
    }

    public static OperationalFlight createDepartedFlight() {
        OperationalFlight flight = createValidFlight();
        flight.setStatus(FlightStatus.DEPARTED);
        flight.setActualDepartureTime(LocalTime.of(10, 35));
        flight.setDepartureDelay(5);
        return flight;
    }

    // Status Update Request Builders
    public static FlightStatusUpdateRequestDto createStatusUpdateRequest(FlightStatus status) {
        FlightStatusUpdateRequestDto request = new FlightStatusUpdateRequestDto();
        request.setStatus(status);
        return request;
    }

    public static FlightStatusUpdateRequestDto createDepartureUpdateRequest() {
        FlightStatusUpdateRequestDto request = new FlightStatusUpdateRequestDto();
        request.setStatus(FlightStatus.DEPARTED);
        request.setActualDepartureTime(LocalTime.of(10, 35));
        return request;
    }

    public static FlightStatusUpdateRequestDto createArrivalUpdateRequest() {
        FlightStatusUpdateRequestDto request = new FlightStatusUpdateRequestDto();
        request.setStatus(FlightStatus.ARRIVED);
        request.setActualArrivalTime(LocalTime.of(13, 50));
        return request;
    }

    public static FlightStatusUpdateRequestDto createDelayedUpdateRequest() {
        FlightStatusUpdateRequestDto request = new FlightStatusUpdateRequestDto();
        request.setStatus(FlightStatus.DELAYED);
        request.setDelayReason("Weather conditions");
        return request;
    }

    public static FlightStatusUpdateRequestDto createCancelledUpdateRequest() {
        FlightStatusUpdateRequestDto request = new FlightStatusUpdateRequestDto();
        request.setStatus(FlightStatus.CANCELLED);
        request.setCancellationReason("Aircraft maintenance");
        return request;
    }

    // Upload Batch Builders
    public static FlightUploadBatch createUploadBatch() {
        return FlightUploadBatch.builder()
                .id(1L)
                .fileName("test-flights.csv")
                .fileSize(1024L)
                .totalRows(10)
                .successfulRows(8)
                .failedRows(1)
                .conflictRows(1)
                .status(UploadStatus.COMPLETED)
                .uploadedBy("testuser")
                .airlineId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static FlightUploadBatch createProcessingBatch() {
        FlightUploadBatch batch = createUploadBatch();
        batch.setStatus(UploadStatus.PROCESSING);
        batch.setProcessingStartTime(LocalDateTime.now());
        return batch;
    }

    // Reference Data Mock Builders
    public static Map<String, Object> createAirlineData() {
        Map<String, Object> airline = new HashMap<>();
        airline.put("id", 1L);
        airline.put("code", "TK");
        airline.put("name", "Turkish Airlines");
        airline.put("country", "Turkey");
        airline.put("isActive", true);
        return airline;
    }

    public static Map<String, Object> createAirlineData(Long id, String code, String name) {
        Map<String, Object> airline = new HashMap<>();
        airline.put("id", id);
        airline.put("code", code);
        airline.put("name", name);
        airline.put("country", "Turkey");
        airline.put("isActive", true);
        return airline;
    }

    public static Map<String, Object> createStationData() {
        Map<String, Object> station = new HashMap<>();
        station.put("id", 1L);
        station.put("icaoCode", "LTBA");
        station.put("iataCode", "IST");
        station.put("name", "Istanbul Airport");
        station.put("city", "Istanbul");
        station.put("country", "Turkey");
        station.put("isActive", true);
        return station;
    }

    public static Map<String, Object> createStationData(Long id, String icaoCode, String name) {
        Map<String, Object> station = new HashMap<>();
        station.put("id", id);
        station.put("icaoCode", icaoCode);
        station.put("iataCode", icaoCode.substring(1));
        station.put("name", name);
        station.put("city", "Test City");
        station.put("country", "Test Country");
        station.put("isActive", true);
        return station;
    }

    public static Map<String, Object> createAircraftData() {
        Map<String, Object> aircraft = new HashMap<>();
        aircraft.put("id", 1L);
        aircraft.put("type", "A320");
        aircraft.put("manufacturer", "Airbus");
        aircraft.put("model", "A320-200");
        aircraft.put("capacity", 180);
        aircraft.put("isActive", true);
        return aircraft;
    }

    public static Map<String, Object> createAircraftData(Long id, String type, String manufacturer) {
        Map<String, Object> aircraft = new HashMap<>();
        aircraft.put("id", id);
        aircraft.put("type", type);
        aircraft.put("manufacturer", manufacturer);
        aircraft.put("model", type + "-200");
        aircraft.put("capacity", 180);
        aircraft.put("isActive", true);
        return aircraft;
    }

    // CSV Data Builders
    public static String createValidCsvContent() {
        return """
                flightNumber,airlineCode,aircraftType,flightDate,scheduledDepartureTime,scheduledArrivalTime,originIcaoCode,destinationIcaoCode,flightType,gate,terminal
                TK123,TK,A320,2025-02-15,10:30,13:45,LTBA,EDDF,PASSENGER,A12,1
                LH456,LH,B737,2025-02-15,14:00,17:30,EDDF,KJFK,PASSENGER,B5,2
                """;
    }

    public static String createInvalidCsvContent() {
        return """
                flightNumber,airlineCode,aircraftType,flightDate,scheduledDepartureTime,scheduledArrivalTime,originIcaoCode,destinationIcaoCode,flightType
                INVALID,XX,A320,invalid-date,25:00,30:00,XXXX,YYYY,INVALID_TYPE
                """;
    }

    public static String createConflictCsvContent() {
        return """
                flightNumber,airlineCode,aircraftType,flightDate,scheduledDepartureTime,scheduledArrivalTime,originIcaoCode,destinationIcaoCode,flightType
                TK123,TK,A320,2025-01-15,10:30,13:45,LTBA,EDDF,PASSENGER
                """;
    }

    // Utility Methods
    public static LocalDate tomorrow() {
        return LocalDate.now().plusDays(1);
    }

    public static LocalDate yesterday() {
        return LocalDate.now().minusDays(1);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static LocalTime departureTime() {
        return LocalTime.of(10, 30);
    }

    public static LocalTime arrivalTime() {
        return LocalTime.of(13, 45);
    }
}