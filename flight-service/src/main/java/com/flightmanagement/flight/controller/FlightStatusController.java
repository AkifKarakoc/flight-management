package com.flightmanagement.flight.controller;

import com.flightmanagement.flight.dto.request.FlightStatusUpdateRequestDto;
import com.flightmanagement.flight.dto.response.DashboardOverviewDto;
import com.flightmanagement.flight.dto.response.LiveFlightStatusDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.security.UserContext;
import com.flightmanagement.flight.service.FlightStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightStatusController {

    private final FlightStatusService flightStatusService;

    @PatchMapping("/{id}/status")
    public ResponseEntity<OperationalFlightResponseDto> updateFlightStatus(
            @PathVariable Long id,
            @Valid @RequestBody FlightStatusUpdateRequestDto request,
            Authentication authentication) {

        UserContext userContext = (UserContext) authentication.getPrincipal();
        OperationalFlightResponseDto response = flightStatusService.updateFlightStatus(id, request, userContext);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/live")
    public ResponseEntity<List<LiveFlightStatusDto>> getLiveFlightStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        UserContext userContext = (UserContext) authentication.getPrincipal();
        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<LiveFlightStatusDto> response = flightStatusService.getLiveFlightStatus(targetDate, userContext);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardOverviewDto> getDashboardOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        UserContext userContext = (UserContext) authentication.getPrincipal();
        LocalDate targetDate = date != null ? date : LocalDate.now();

        DashboardOverviewDto response = flightStatusService.getDashboardOverview(targetDate, userContext);
        return ResponseEntity.ok(response);
    }
}