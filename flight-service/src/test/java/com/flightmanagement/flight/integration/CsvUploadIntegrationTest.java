package com.flightmanagement.flight.integration;

import com.flightmanagement.flight.entity.FlightUploadBatch;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.UploadStatus;
import com.flightmanagement.flight.repository.FlightUploadBatchRepository;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.util.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvUploadIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FlightUploadBatchRepository batchRepository;

    @Autowired
    private OperationalFlightRepository flightRepository;

    @Test
    void uploadCsvFile_ValidFile_Success() throws InterruptedException {
        // Given
        String csvContent = TestDataBuilder.createValidCsvContent();
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-flights.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth("mock-airline-token");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights/upload",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).contains("File upload started");

        // Wait for async processing to complete
        Thread.sleep(2000);

        // Verify upload batch was created
        List<FlightUploadBatch> batches = batchRepository.findAll();
        assertThat(batches).isNotEmpty();

        FlightUploadBatch batch = batches.get(0);
        assertThat(batch.getFileName()).isEqualTo("test-flights.csv");
        assertThat(batch.getStatus()).isIn(UploadStatus.PROCESSING, UploadStatus.COMPLETED);

        // If processing completed successfully, verify flights were created
        if (batch.getStatus() == UploadStatus.COMPLETED) {
            List<OperationalFlight> flights = flightRepository.findAll();
            assertThat(flights).isNotEmpty();
            assertThat(flights.get(0).getUploadBatchId()).isEqualTo(batch.getId());
        }
    }

    @Test
    void uploadCsvFile_InvalidFileType_ReturnsBadRequest() {
        // Given
        ByteArrayResource fileResource = new ByteArrayResource("invalid content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth("mock-airline-token");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights/upload",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Only CSV files are allowed");
    }

    @Test
    void uploadCsvFile_EmptyFile_ReturnsBadRequest() {
        // Given
        ByteArrayResource fileResource = new ByteArrayResource(new byte[0]) {
            @Override
            public String getFilename() {
                return "empty.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth("mock-airline-token");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights/upload",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("File is empty");
    }

    @Test
    void uploadCsvFile_LargeFile_ReturnsBadRequest() {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB (exceeds 10MB limit)
        ByteArrayResource fileResource = new ByteArrayResource(largeContent) {
            @Override
            public String getFilename() {
                return "large.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth("mock-airline-token");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights/upload",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("File size exceeds 10MB limit");
    }

    @Test
    void uploadCsvFile_NoAuthentication_ReturnsUnauthorized() {
        // Given
        String csvContent = TestDataBuilder.createValidCsvContent();
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // No authentication

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/flights/upload",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}