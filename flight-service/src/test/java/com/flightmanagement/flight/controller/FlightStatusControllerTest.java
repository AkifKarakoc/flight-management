package com.flightmanagement.flight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightmanagement.flight.config.FlightTestConfiguration;
import com.flightmanagement.flight.dto.request.FlightStatusUpdateRequestDto;
import com.flightmanagement.flight.dto.response.DashboardOverviewDto;
import com.flightmanagement.flight.dto.response.LiveFlightStatusDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.enums.FlightStatus;
import com.flightmanagement.flight.exception.FlightNotFoundException;
import com.flightmanagement.flight.security.UserContext;
import com.flightmanagement.flight.service.FlightStatusService;
import com.flightmanagement.flight.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlightStatusController.class)
@Import(FlightTestConfiguration.class)
@ActiveProfiles("test")
class FlightStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FlightStatusService flightStatusService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserContext adminContext;
    private UsernamePasswordAuthenticationToken adminAuth;
    private FlightStatusUpdateRequestDto statusUpdateRequest;
    private OperationalFlightResponseDto flightResponse;

    @BeforeEach
    void setUp() {
        adminContext = TestDataBuilder.createAdminUserContext();
        adminAuth = new UsernamePasswordAuthenticationToken(
                adminContext, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        statusUpdateRequest = TestDataBuilder.createDepartureUpdateRequest();

        flightResponse = new OperationalFlightResponseDto();
        flightResponse.setId(1L);
        flightResponse.setFlightNumber("TK123");
        flightResponse.setStatus(FlightStatus.DEPARTED);
        flightResponse.setActualDepartureTime(LocalTime.of(10, 35));
        flightResponse.setDepartureDelay(5);
    }

    @Test
    void updateFlightStatus_ValidRequest_ReturnsOk() throws Exception {
        // Given
        when(flightStatusService.updateFlightStatus(eq(1L), any(), any())).thenReturn(flightResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/flights/1/status")
                        .with(authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DEPARTED"))
                .andExpect(jsonPath("$.actualDepartureTime").value("10:35:00"))
                .andExpect(jsonPath("$.departureDelay").value(5));
    }

    @Test
    void updateFlightStatus_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        FlightStatusUpdateRequestDto invalidRequest = new FlightStatusUpdateRequestDto();
        // Missing required status field

        // When & Then
        mockMvc.perform(patch("/api/v1/flights/1/status")
                        .with(authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.status").exists());
    }

    @Test
    void updateFlightStatus_NonExistingFlight_ReturnsNotFound() throws Exception {
        // Given
        when(flightStatusService.updateFlightStatus(eq(999L), any(), any()))
                .thenThrow(new FlightNotFoundException("Flight not found"));

        // When & Then
        mockMvc.perform(patch("/api/v1/flights/999/status")
                        .with(authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flight not found"));
    }

    @Test
    void getLiveFlightStatus_ValidRequest_ReturnsOk() throws Exception {
        // Given
        LiveFlightStatusDto liveStatus = new LiveFlightStatusDto();
        liveStatus.setId(1L);
        liveStatus.setFlightNumber("TK123");
        liveStatus.setStatus(FlightStatus.DEPARTED);
        liveStatus.setIsOnTime(false);
        liveStatus.setDepartureDelay(5);

        when(flightStatusService.getLiveFlightStatus(any(), any())).thenReturn(List.of(liveStatus));

        // When & Then
        mockMvc.perform(get("/api/v1/flights/live")
                        .with(authentication(adminAuth))
                        .param("date", "2025-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].flightNumber").value("TK123"))
                .andExpect(jsonPath("$[0].status").value("DEPARTED"))
                .andExpect(jsonPath("$[0].isOnTime").value(false))
                .andExpect(jsonPath("$[0].departureDelay").value(5));
    }

    @Test
    void getLiveFlightStatus_NoDateParam_UsesToday() throws Exception {
        // Given
        when(flightStatusService.getLiveFlightStatus(eq(LocalDate.now()), any()))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/flights/live")
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getDashboardOverview_ValidRequest_ReturnsOk() throws Exception {
        // Given
        DashboardOverviewDto dashboard = DashboardOverviewDto.builder()
                .date(LocalDate.now())
                .totalFlights(10)
                .scheduledFlights(6)
                .departedFlights(2)
                .arrivedFlights(1)
                .cancelledFlights(1)
                .averageDelay(15.0)
                .onTimePerformance(80.0)
                .activeAirlines(3)
                .busiestRoute("LTBA-EDDF")
                .topDelayReasons(List.of("Weather", "ATC", "Technical"))
                .build();

        when(flightStatusService.getDashboardOverview(any(), any())).thenReturn(dashboard);

        // When & Then
        mockMvc.perform(get("/api/v1/flights/dashboard")
                        .with(authentication(adminAuth))
                        .param("date", "2025-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFlights").value(10))
                .andExpect(jsonPath("$.scheduledFlights").value(6))
                .andExpect(jsonPath("$.departedFlights").value(2))
                .andExpect(jsonPath("$.arrivedFlights").value(1))
                .andExpect(jsonPath("$.cancelledFlights").value(1))
                .andExpect(jsonPath("$.averageDelay").value(15.0))
                .andExpect(jsonPath("$.onTimePerformance").value(80.0))
                .andExpect(jsonPath("$.activeAirlines").value(3))
                .andExpect(jsonPath("$.busiestRoute").value("LTBA-EDDF"))
                .andExpect(jsonPath("$.topDelayReasons").isArray())
                .andExpect(jsonPath("$.topDelayReasons[0]").value("Weather"));
    }

    @Test
    void getDashboardOverview_NoDateParam_UsesToday() throws Exception {
        // Given
        DashboardOverviewDto dashboard = DashboardOverviewDto.builder()
                .date(LocalDate.now())
                .totalFlights(0)
                .build();

        when(flightStatusService.getDashboardOverview(eq(LocalDate.now()), any()))
                .thenReturn(dashboard);

        // When & Then
        mockMvc.perform(get("/api/v1/flights/dashboard")
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.totalFlights").value(0));
    }

    @Test
    void updateFlightStatus_NoAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/v1/flights/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isUnauthorized());
    }
}