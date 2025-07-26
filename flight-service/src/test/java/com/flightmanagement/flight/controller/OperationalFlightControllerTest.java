package com.flightmanagement.flight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.enums.FlightType;
import com.flightmanagement.flight.security.UserContext;
import com.flightmanagement.flight.service.OperationalFlightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OperationalFlightController.class)
@ActiveProfiles("test")
class OperationalFlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationalFlightService flightService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createFlight_ValidRequest_ReturnsCreated() throws Exception {
        // Given
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

        OperationalFlightResponseDto response = new OperationalFlightResponseDto();
        response.setId(1L);
        response.setFlightNumber("TK123");

        UserContext userContext = UserContext.builder()
                .userId(1L)
                .username("admin")
                .roles(List.of("ROLE_ADMIN"))
                .build();

        when(flightService.createFlight(any(), any())).thenReturn(response);

        // Create authentication with UserContext as principal
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userContext, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // When & Then
        mockMvc.perform(post("/api/v1/flights")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.flightNumber").value("TK123"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createFlight_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given - empty request (missing required fields)
        OperationalFlightCreateRequestDto request = new OperationalFlightCreateRequestDto();

        UserContext userContext = UserContext.builder()
                .userId(1L)
                .username("admin")
                .roles(List.of("ROLE_ADMIN"))
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userContext, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // When & Then
        mockMvc.perform(post("/api/v1/flights")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}