package com.flightmanagement.reference.controller;

import com.flightmanagement.reference.dto.request.AirlineCreateRequestDto;
import com.flightmanagement.reference.dto.response.AirlineResponseDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.service.AirlineService;
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
public class AirlineController {

    private final AirlineService airlineService;

    @GetMapping
    public ResponseEntity<PagedResponse<AirlineResponseDto>> getAllAirlines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<AirlineResponseDto> response = airlineService.getAllAirlines(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirlineResponseDto> getAirlineById(@PathVariable Long id) {
        AirlineResponseDto response = airlineService.getAirlineById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirlineResponseDto> createAirline(@Valid @RequestBody AirlineCreateRequestDto request) {
        AirlineResponseDto response = airlineService.createAirline(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirlineResponseDto> updateAirline(@PathVariable Long id,
                                                            @Valid @RequestBody AirlineCreateRequestDto request) {
        AirlineResponseDto response = airlineService.updateAirline(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAirline(@PathVariable Long id) {
        airlineService.deleteAirline(id);
        return ResponseEntity.ok("Airline deleted successfully");
    }
}