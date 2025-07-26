package com.flightmanagement.reference.controller;

import com.flightmanagement.reference.dto.request.AirlineCreateRequestDto;
import com.flightmanagement.reference.dto.response.AirlineResponseDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.service.AirlineService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/airlines")
@RequiredArgsConstructor
@Tag(name = "Airlines", description = "Airline management operations")
@SecurityRequirement(name = "bearerAuth")
public class AirlineController {

    private final AirlineService airlineService;

    @Operation(summary = "Get all airlines", description = "Retrieve airlines with pagination")
    @ApiResponse(responseCode = "200", description = "Airlines retrieved successfully")
    @GetMapping
    public ResponseEntity<PagedResponse<AirlineResponseDto>> getAllAirlines(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<AirlineResponseDto> response = airlineService.getAllAirlines(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get airline by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Airline found"),
            @ApiResponse(responseCode = "404", description = "Airline not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AirlineResponseDto> getAirlineById(@PathVariable Long id) {
        AirlineResponseDto response = airlineService.getAirlineById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create airline", description = "Admin only operation")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirlineResponseDto> createAirline(@Valid @RequestBody AirlineCreateRequestDto request) {
        AirlineResponseDto response = airlineService.createAirline(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update airline", description = "Admin only operation")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirlineResponseDto> updateAirline(@PathVariable Long id,
                                                            @Valid @RequestBody AirlineCreateRequestDto request) {
        AirlineResponseDto response = airlineService.updateAirline(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete airline", description = "Soft delete - Admin only")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAirline(@PathVariable Long id) {
        airlineService.deleteAirline(id);
        return ResponseEntity.ok("Airline deleted successfully");
    }
}