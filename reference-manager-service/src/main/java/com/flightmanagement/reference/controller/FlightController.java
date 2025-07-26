package com.flightmanagement.reference.controller;

import com.flightmanagement.reference.dto.request.FlightCreateRequestDto;
import com.flightmanagement.reference.dto.response.FlightResponseDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.security.UserContext;
import com.flightmanagement.reference.security.UserPrincipal;
import com.flightmanagement.reference.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Flights", description = "Flight management operations")
@SecurityRequirement(name = "bearerAuth")
public class FlightController {

    private final FlightService flightService;

    @Operation(summary = "Get flights",
            description = "Retrieve flights with pagination and filtering. Airline users see only their flights.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flights retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<FlightResponseDto>> getFlights(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by flight date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDate,
            @Parameter(description = "Filter by airline ID") @RequestParam(required = false) Long airlineId,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        UserContext userContext = extractUserContext(authentication);

        PagedResponse<FlightResponseDto> response = flightService.getFlights(pageable, userContext, flightDate, airlineId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get flight by ID",
            description = "Retrieve specific flight details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight found"),
            @ApiResponse(responseCode = "404", description = "Flight not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FlightResponseDto> getFlightById(
            @Parameter(description = "Flight ID") @PathVariable Long id,
            Authentication authentication) {

        UserContext userContext = extractUserContext(authentication);
        FlightResponseDto response = flightService.getFlightById(id, userContext);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create new flight",
            description = "Create a new flight with route segments")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Flight created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Flight already exists"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('AIRLINE_USER') and @flightService.canAccessAirline(#request.airlineId, authentication.name))")
    public ResponseEntity<FlightResponseDto> createFlight(
            @Valid @RequestBody FlightCreateRequestDto request,
            Authentication authentication) {

        UserContext userContext = extractUserContext(authentication);
        FlightResponseDto response = flightService.createFlight(request, userContext);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update flight",
            description = "Update existing flight and route segments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Flight not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping("/{id}")
    public ResponseEntity<FlightResponseDto> updateFlight(
            @Parameter(description = "Flight ID") @PathVariable Long id,
            @Valid @RequestBody FlightCreateRequestDto request,
            Authentication authentication) {

        UserContext userContext = extractUserContext(authentication);
        FlightResponseDto response = flightService.updateFlight(id, request, userContext);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete flight",
            description = "Soft delete flight and its route segments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Flight not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFlight(
            @Parameter(description = "Flight ID") @PathVariable Long id,
            Authentication authentication)   {
        UserContext userContext = extractUserContext(authentication);
        flightService.deleteFlight(id, userContext);
        return ResponseEntity.ok("Flight deleted successfully");
    }

    private UserContext extractUserContext(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return UserContext.builder()
                .userId(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .airlineId(userPrincipal.getAirlineId())
                .roles(userPrincipal.getAuthorities().stream()
                        .map(Object::toString).toList())
                .build();
    }
}