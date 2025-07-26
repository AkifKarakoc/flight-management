package com.flightmanagement.flight.integration;

import com.flightmanagement.flight.repository.FlightUploadBatchRepository;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class CsvUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlightUploadBatchRepository batchRepository;

    @Autowired
    private OperationalFlightRepository flightRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadCsvFile_ValidFile_ReturnsAccepted() throws Exception {
        // Given
        String csvContent = """
                flightNumber,airlineCode,aircraftType,flightDate,scheduledDepartureTime,scheduledArrivalTime,originIcaoCode,destinationIcaoCode,flightType
                TK123,TK,A320,2025-02-15,10:30,13:45,LTBA,EDDF,PASSENGER
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-flights.csv",
                "text/csv",
                csvContent.getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/v1/flights/upload")
                        .file(file))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadCsvFile_InvalidFileType_ReturnsBadRequest() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "invalid content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/v1/flights/upload")
                        .file(file))
                .andExpected(status().isBadRequest());
    }
}