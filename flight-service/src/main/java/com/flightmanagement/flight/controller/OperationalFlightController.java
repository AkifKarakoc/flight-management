package com.flightmanagement.flight.controller;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.dto.response.PagedResponse;
import com.flightmanagement.flight.security.UserContext;
import com.flightmanagement.flight.service.OperationalFlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class OperationalFlightController {

    private final OperationalFlightService flightService;

    @GetMapping
    public ResponseEntity<PagedResponse<OperationalFlightResponseDto>> getFlights(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        UserContext userContext = (UserContext) authentication.getPrincipal();

        PagedResponse<OperationalFlightResponseDto> response = flightService.getFlights(pageable, userContext);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperationalFlightResponseDto> getFlightById(
            @PathVariable Long id,
            Authentication authentication) {

        UserContext userContext = (UserContext) authentication.getPrincipal();
        OperationalFlightResponseDto response = flightService.getFlightById(id, userContext);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<OperationalFlightResponseDto> createFlight(
            @Valid @RequestBody OperationalFlightCreateRequestDto request,
            Authentication authentication) {

        UserContext userContext = (UserContext) authentication.getPrincipal();
        OperationalFlightResponseDto response = flightService.createFlight(request, userContext);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OperationalFlightResponseDto> updateFlight(
            @PathVariable Long id,
            @Valid @RequestBody OperationalFlightCreateRequestDto request,
            Authentication authentication) {

        UserContext userContext = (UserContext) authentication.getPrincipal();
        OperationalFlightResponseDto response = flightService.updateFlight(id, request, userContext);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFlight(
            @PathVariable Long id,
            Authentication authentication) {

        UserContext userContext = (UserContext) authentication.getPrincipal();
        flightService.deleteFlight(id, userContext);
        return ResponseEntity.ok("Flight deleted successfully");
    }
}