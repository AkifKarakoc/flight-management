package com.flightmanagement.reference.controller;

import com.flightmanagement.reference.dto.request.StationCreateRequestDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.dto.response.StationResponseDto;
import com.flightmanagement.reference.service.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping
    public ResponseEntity<PagedResponse<StationResponseDto>> getAllStations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<StationResponseDto> response = stationService.getAllStations(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationResponseDto> getStationById(@PathVariable Long id) {
        StationResponseDto response = stationService.getStationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<StationResponseDto>> searchStations(@RequestParam String query) {
        List<StationResponseDto> response = stationService.searchStations(query);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationResponseDto> createStation(@Valid @RequestBody StationCreateRequestDto request) {
        StationResponseDto response = stationService.createStation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationResponseDto> updateStation(@PathVariable Long id,
                                                            @Valid @RequestBody StationCreateRequestDto request) {
        StationResponseDto response = stationService.updateStation(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.ok("Station deleted successfully");
    }
}