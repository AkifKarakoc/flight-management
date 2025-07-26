package com.flightmanagement.reference.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightmanagement.reference.dto.request.FlightCreateRequestDto;
import com.flightmanagement.reference.dto.response.FlightResponseDto;
import com.flightmanagement.reference.enums.FlightType;
import com.flightmanagement.reference.service.FlightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlightController.class)
@Import(FlightService.class)
class FlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private FlightService flightService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createFlight_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        FlightCreateRequestDto request = new FlightCreateRequestDto();
        request.setFlightNumber("TK123");
        request.setAirlineId(1L);
        request.setAircraftId(1L);
        request.setFlightDate(LocalDate.now().plusDays(1));
        request.setDepartureTime(LocalTime.of(10, 30));
        request.setArrivalTime(LocalTime.of(13, 45));
        request.setFlightType(FlightType.PASSENGER);
        request.setSegments(Collections.emptyList());

        FlightResponseDto response = new FlightResponseDto();
        response.setId(1L);
        response.setFlightNumber("TK123");

        when(flightService.createFlight(any(), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/flights")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.flightNumber").value("TK123"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createFlight_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        FlightCreateRequestDto request = new FlightCreateRequestDto();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/flights")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}