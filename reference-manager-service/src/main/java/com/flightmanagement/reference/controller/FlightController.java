package com.flightmanagement.reference.controller;

import com.flightmanagement.reference.dto.request.FlightCreateRequestDto;
import com.flightmanagement.reference.dto.response.FlightResponseDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.security.UserContext;
import com.flightmanagement.reference.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping
    public ResponseEntity<PagedResponse<FlightResponseDto>> getFlights(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDate,
            @RequestParam(required = false) Long airlineId,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        UserContext userContext = extractUserContext(authentication);

        PagedResponse<FlightResponseDto> response = flightService.getFlights(pageable, userContext, flightDate, airlineId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightResponseDto> getFlightById(@PathVariable Long id, Authentication authentication) {
        UserContext userContext = extractUserContext(authentication);
        FlightResponseDto response = flightService.getFlightById(id, userContext);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('AIRLINE_USER') and @flightService.canAccessAirline(#request.airlineId, authentication.name))")
    public ResponseEntity<FlightResponseDto> createFlight(
            @Valid @RequestBody FlightCreateRequestDto request,
            Authentication authentication) {

        UserContext userContext = extractUserContext(authentication);
        FlightResponseDto response = flightService.createFlight(request, userContext);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlightResponseDto> updateFlight(
            @PathVariable Long id,
            @Valid @RequestBody FlightCreateRequestDto request,
            Authentication authentication) {

        UserContext userContext = extractUserContext(authentication);
        FlightResponseDto response = flightService.updateFlight(id, request, userContext);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFlight(@PathVariable Long id, Authentication authentication) {
        UserContext userContext = extractUserContext(authentication);
        flightService.deleteFlight(id, userContext);
        return ResponseEntity.ok("Flight deleted successfully");
    }

    private UserContext extractUserContext(Authentication authentication) {
        return UserContext.builder()
                .username(authentication.getName())
                .airlineId(1L) // This should be extracted from JWT
                .roles(authentication.getAuthorities().stream()
                        .map(Object::toString).toList())
                .build();
    }
}