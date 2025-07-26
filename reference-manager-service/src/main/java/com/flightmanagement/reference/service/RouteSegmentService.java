package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.request.RouteSegmentCreateRequestDto;
import com.flightmanagement.reference.dto.response.RouteSegmentResponseDto;
import com.flightmanagement.reference.entity.Flight;
import com.flightmanagement.reference.entity.RouteSegment;
import com.flightmanagement.reference.exception.ReferenceNotFoundException;
import com.flightmanagement.reference.mapper.RouteSegmentMapper;
import com.flightmanagement.reference.repository.RouteSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteSegmentService {

    private final RouteSegmentRepository segmentRepository;
    private final RouteSegmentMapper segmentMapper;

    public List<RouteSegmentResponseDto> getSegmentsByFlightId(Long flightId) {
        List<RouteSegment> segments = segmentRepository.findByFlightIdOrderBySegmentOrder(flightId);
        return segments.stream().map(segmentMapper::toResponseDto).toList();
    }

    public RouteSegmentResponseDto createSegment(Long flightId, RouteSegmentCreateRequestDto request) {
        // Validate segment order
        if (segmentRepository.existsByFlightIdAndSegmentOrder(flightId, request.getSegmentOrder())) {
            throw new IllegalArgumentException("Segment order already exists for this flight");
        }

        RouteSegment segment = segmentMapper.toEntity(request);
        segment.setFlight(Flight.builder().id(flightId).build());
        segment = segmentRepository.save(segment);

        return segmentMapper.toResponseDto(segment);
    }

    public void createSegmentsForFlight(Flight flight, List<RouteSegmentCreateRequestDto> segmentDtos) {
        for (RouteSegmentCreateRequestDto segmentDto : segmentDtos) {
            RouteSegment segment = segmentMapper.toEntity(segmentDto);
            segment.setFlight(flight);
            segmentRepository.save(segment);
        }
    }

    public void updateSegmentsForFlight(Flight flight, List<RouteSegmentCreateRequestDto> segmentDtos) {
        // Delete existing segments
        deleteSegmentsByFlightId(flight.getId());

        // Create new segments
        createSegmentsForFlight(flight, segmentDtos);
    }

    public RouteSegmentResponseDto updateSegment(Long segmentId, RouteSegmentCreateRequestDto request) {
        RouteSegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ReferenceNotFoundException("Route segment not found with id: " + segmentId));

        segmentMapper.updateEntityFromDto(request, segment);
        segment = segmentRepository.save(segment);

        return segmentMapper.toResponseDto(segment);
    }

    public void deleteSegment(Long segmentId) {
        RouteSegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ReferenceNotFoundException("Route segment not found with id: " + segmentId));

        segment.setIsActive(false);
        segmentRepository.save(segment);
    }

    public void deleteSegmentsByFlightId(Long flightId) {
        List<RouteSegment> segments = segmentRepository.findByFlightIdOrderBySegmentOrder(flightId);
        segments.forEach(segment -> segment.setIsActive(false));
        segmentRepository.saveAll(segments);
    }
}