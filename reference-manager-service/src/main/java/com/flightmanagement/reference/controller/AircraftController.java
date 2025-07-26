package com.flightmanagement.reference.controller;

import com.flightmanagement.reference.dto.request.AircraftCreateRequestDto;
import com.flightmanagement.reference.dto.response.AircraftResponseDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.service.AircraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/aircraft")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftService aircraftService;

    @GetMapping
    public ResponseEntity<PagedResponse<AircraftResponseDto>> getAllAircraft(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<AircraftResponseDto> response = aircraftService.getAllAircraft(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AircraftResponseDto> getAircraftById(@PathVariable Long id) {
        AircraftResponseDto response = aircraftService.getAircraftById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AircraftResponseDto> createAircraft(@Valid @RequestBody AircraftCreateRequestDto request) {
        AircraftResponseDto response = aircraftService.createAircraft(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AircraftResponseDto> updateAircraft(@PathVariable Long id,
                                                              @Valid @RequestBody AircraftCreateRequestDto request) {
        AircraftResponseDto response = aircraftService.updateAircraft(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAircraft(@PathVariable Long id) {
        aircraftService.deleteAircraft(id);
        return ResponseEntity.ok("Aircraft deleted successfully");
    }
}