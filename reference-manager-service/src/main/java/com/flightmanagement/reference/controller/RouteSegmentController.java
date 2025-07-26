package com.flightmanagement.reference.controller;

import com.flightmanagement.reference.dto.request.RouteSegmentCreateRequestDto;
import com.flightmanagement.reference.dto.response.RouteSegmentResponseDto;
import com.flightmanagement.reference.service.RouteSegmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RouteSegmentController {

    private final RouteSegmentService segmentService;

    @GetMapping("/flights/{flightId}/segments")
    public ResponseEntity<List<RouteSegmentResponseDto>> getSegmentsByFlightId(@PathVariable Long flightId) {
        List<RouteSegmentResponseDto> response = segmentService.getSegmentsByFlightId(flightId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/flights/{flightId}/segments")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AIRLINE_USER')")
    public ResponseEntity<RouteSegmentResponseDto> createSegment(
            @PathVariable Long flightId,
            @Valid @RequestBody RouteSegmentCreateRequestDto request) {

        RouteSegmentResponseDto response = segmentService.createSegment(flightId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/segments/{segmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AIRLINE_USER')")
    public ResponseEntity<RouteSegmentResponseDto> updateSegment(
            @PathVariable Long segmentId,
            @Valid @RequestBody RouteSegmentCreateRequestDto request) {

        RouteSegmentResponseDto response = segmentService.updateSegment(segmentId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/segments/{segmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AIRLINE_USER')")
    public ResponseEntity<String> deleteSegment(@PathVariable Long segmentId) {
        segmentService.deleteSegment(segmentId);
        return ResponseEntity.ok("Route segment deleted successfully");
    }
}