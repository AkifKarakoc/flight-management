package com.flightmanagement.flight.integration;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.dto.response.PagedResponse;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.FlightStatus;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.util.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalFlightIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OperationalFlightRepository flightRepository;

    @Test
    void createFlight_ValidRequest_Success() {
        // Given
        OperationalFlightCreateRequestDto request = TestDataBuilder.createValidFlightRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("mock-admin-token"); // This would be a real JWT in production

        HttpEntity<OperationalFlightCreateRequestDto> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<OperationalFlightResponseDto> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights",
                HttpMethod.POST,
                entity,
                OperationalFlightResponseDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFlightNumber()).isEqualTo("TK123");
        assertThat(response.getBody().getStatus()).isEqualTo(FlightStatus.SCHEDULED);

        // Verify database
        OperationalFlight savedFlight = flightRepository.findById(response.getBody().getId()).orElse(null);
        assertThat(savedFlight).isNotNull();
        assertThat(savedFlight.getFlightNumber()).isEqualTo("TK123");
        assertThat(savedFlight.getIsActive()).isTrue();
    }

    @Test
    void getFlights_WithPagination_Success() {
        // Given - Create test data
        OperationalFlight flight1 = TestDataBuilder.createFlight(1L, "TK123", 1L);
        OperationalFlight flight2 = TestDataBuilder.createFlight(2L, "TK456", 1L);
        flightRepository.save(flight1);
        flightRepository.save(flight2);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("mock-admin-token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<PagedResponse<OperationalFlightResponseDto>> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights?page=0&size=10",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<PagedResponse<OperationalFlightResponseDto>>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getTotalElements()).isEqualTo(2);
    }

    @Test
    void getFlightById_ExistingFlight_Success() {
        // Given
        OperationalFlight flight = TestDataBuilder.createValidFlight();
        flight = flightRepository.save(flight);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("mock-admin-token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<OperationalFlightResponseDto> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights/" + flight.getId(),
                HttpMethod.GET,
                entity,
                OperationalFlightResponseDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(flight.getId());
        assertThat(response.getBody().getFlightNumber()).isEqualTo(flight.getFlightNumber());
    }

    @Test
    void getFlightById_NonExistingFlight_ReturnsNotFound() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("mock-admin-token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights/999",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateFlight_ValidRequest_Success() {
        // Given
        OperationalFlight flight = TestDataBuilder.createValidFlight();
        flight = flightRepository.save(flight);

        OperationalFlightCreateRequestDto updateRequest = TestDataBuilder.createValidFlightRequest();
        updateRequest.setFlightNumber("TK999");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("mock-admin-token");
        HttpEntity<OperationalFlightCreateRequestDto> entity = new HttpEntity<>(updateRequest, headers);

        // When
        ResponseEntity<OperationalFlightResponseDto> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights/" + flight.getId(),
                HttpMethod.PUT,
                entity,
                OperationalFlightResponseDto.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFlightNumber()).isEqualTo("TK999");

        // Verify database
        OperationalFlight updatedFlight = flightRepository.findById(flight.getId()).orElse(null);
        assertThat(updatedFlight).isNotNull();
        assertThat(updatedFlight.getFlightNumber()).isEqualTo("TK999");
    }

    @Test
    void deleteFlight_ExistingFlight_Success() {
        // Given
        OperationalFlight flight = TestDataBuilder.createValidFlight();
        flight = flightRepository.save(flight);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("mock-admin-token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights/" + flight.getId(),
                HttpMethod.DELETE,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Flight deleted successfully");

        // Verify soft delete
        OperationalFlight deletedFlight = flightRepository.findById(flight.getId()).orElse(null);
        assertThat(deletedFlight).isNotNull();
        assertThat(deletedFlight.getIsActive()).isFalse();
    }

    @Test
    void createFlight_InvalidRequest_ReturnsBadRequest() {
        // Given
        OperationalFlightCreateRequestDto invalidRequest = new OperationalFlightCreateRequestDto();
        // Missing required fields

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("mock-admin-token");
        HttpEntity<OperationalFlightCreateRequestDto> entity = new HttpEntity<>(invalidRequest, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}