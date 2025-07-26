package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.request.FlightCreateRequestDto;
import com.flightmanagement.reference.dto.response.FlightResponseDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.entity.Flight;
import com.flightmanagement.reference.exception.DuplicateReferenceException;
import com.flightmanagement.reference.exception.ReferenceNotFoundException;
import com.flightmanagement.reference.exception.UnauthorizedAccessException;
import com.flightmanagement.reference.mapper.FlightMapper;
import com.flightmanagement.reference.repository.FlightRepository;
import com.flightmanagement.reference.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;


import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;
    private final EventPublishService eventPublishService;
    private final RouteSegmentService routeSegmentService;
    private final AirlineService airlineService;
    private final AircraftService aircraftService;
    private final AuditService auditService;


    public PagedResponse<FlightResponseDto> getFlights(Pageable pageable, UserContext userContext,
                                                       LocalDate flightDate, Long airlineId) {
        Page<Flight> flights;

        if (userContext.isAirlineUser()) {
            // Airline users can only see their own flights
            Long userAirlineId = userContext.getAirlineId();
            if (flightDate != null) {
                flights = flightRepository.findByAirlineIdAndFlightDate(userAirlineId, flightDate, pageable);
            } else {
                flights = flightRepository.findByAirlineId(userAirlineId, pageable);
            }
        } else {
            // Admin can see all flights with optional filters
            if (airlineId != null && flightDate != null) {
                flights = flightRepository.findByAirlineIdAndFlightDate(airlineId, flightDate, pageable);
            } else if (airlineId != null) {
                flights = flightRepository.findByAirlineId(airlineId, pageable);
            } else if (flightDate != null) {
                flights = flightRepository.findByFlightDate(flightDate, pageable);
            } else {
                flights = flightRepository.findAll(pageable);
            }
        }

        return createPagedResponse(flights);
    }


    public FlightResponseDto getFlightById(Long id, UserContext userContext) {
        Flight flight = getFlightWithAccessCheck(id, userContext);
        FlightResponseDto response = flightMapper.toResponseDto(flight);

        // Set airline and aircraft details
        response.setAirline(airlineService.getAirlineById(flight.getAirlineId()));
        response.setAircraft(aircraftService.getAircraftById(flight.getAircraftId()));

        return response;
    }

    public FlightResponseDto createFlight(FlightCreateRequestDto request, UserContext userContext) {
        // Validate airline access
        validateAirlineAccess(request.getAirlineId(), userContext);

        // Check for duplicate flight
        if (flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(
                request.getFlightNumber(), request.getAirlineId(), request.getFlightDate())) {
            throw new DuplicateReferenceException("Flight already exists for this airline on this date");
        }

        // Create flight
        Flight flight = flightMapper.toEntity(request);
        flight = flightRepository.save(flight);

        // Create route segments
        if (request.getSegments() != null && !request.getSegments().isEmpty()) {
            routeSegmentService.createSegmentsForFlight(flight, request.getSegments());
        }

        // Audit log
        auditService.logAction("FLIGHT", flight.getId(), "CREATE", userContext, null, flight);

        eventPublishService.publishReferenceEvent("CREATED", "FLIGHT", flight.getId(), flight.getAirlineId());

        FlightResponseDto response = flightMapper.toResponseDto(flight);
        response.setAirline(airlineService.getAirlineById(flight.getAirlineId()));
        response.setAircraft(aircraftService.getAircraftById(flight.getAircraftId()));
        return response;
    }

    public boolean canAccessAirline(Long airlineId, String username) {
        // Admin can access any airline
        // For airline users, check if they belong to the airline
        // This is a simplified check - in real implementation you'd get user from repository
        return true; // For now, allow all access
    }

    public FlightResponseDto updateFlight(Long id, FlightCreateRequestDto request, UserContext userContext) {
        Flight flight = getFlightWithAccessCheck(id, userContext);

        // Old data for audit - sadece önemli alanları kopyalayalım
        String oldFlightData = String.format("FlightNumber: %s, Date: %s, Departure: %s, Arrival: %s",
                flight.getFlightNumber(), flight.getFlightDate(),
                flight.getDepartureTime(), flight.getArrivalTime());

        // Check for duplicate if flight number or date changed
        if (!flight.getFlightNumber().equals(request.getFlightNumber()) ||
                !flight.getFlightDate().equals(request.getFlightDate())) {

            if (flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(
                    request.getFlightNumber(), request.getAirlineId(), request.getFlightDate())) {
                throw new DuplicateReferenceException("Flight already exists for this airline on this date");
            }
        }

        flightMapper.updateEntityFromDto(request, flight);
        flight = flightRepository.save(flight);

        // Update route segments
        if (request.getSegments() != null) {
            routeSegmentService.updateSegmentsForFlight(flight, request.getSegments());
        }

        // Audit log - string olarak geçelim
        auditService.logAction("FLIGHT", flight.getId(), "UPDATE", userContext, oldFlightData, flight);

        eventPublishService.publishReferenceEvent("UPDATED", "FLIGHT", flight.getId(), flight.getAirlineId());

        FlightResponseDto response = flightMapper.toResponseDto(flight);
        response.setAirline(airlineService.getAirlineById(flight.getAirlineId()));
        response.setAircraft(aircraftService.getAircraftById(flight.getAircraftId()));
        return response;
    }

    public void deleteFlight(Long id, UserContext userContext) {
        Flight flight = getFlightWithAccessCheck(id, userContext);

        flight.setIsActive(false);
        flightRepository.save(flight);

        // Soft delete segments
        routeSegmentService.deleteSegmentsByFlightId(flight.getId());

        auditService.logAction("FLIGHT", flight.getId(), "DELETE", userContext, flight, null);

        eventPublishService.publishReferenceEvent("DELETED", "FLIGHT", flight.getId(), flight.getAirlineId());
    }

    private Flight getFlightWithAccessCheck(Long id, UserContext userContext) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ReferenceNotFoundException("Flight not found with id: " + id));

        if (userContext.isAirlineUser() && !flight.getAirlineId().equals(userContext.getAirlineId())) {
            throw new UnauthorizedAccessException("Cannot access flights from different airline");
        }

        return flight;
    }

    private void validateAirlineAccess(Long airlineId, UserContext userContext) {
        if (userContext.isAirlineUser() && !airlineId.equals(userContext.getAirlineId())) {
            throw new UnauthorizedAccessException("Cannot create flight for different airline");
        }
    }

    // FlightService.java içindeki createPagedResponse metodunu güncelleyin:

    private PagedResponse<FlightResponseDto> createPagedResponse(Page<Flight> page) {
        return PagedResponse.<FlightResponseDto>builder()
                .content(page.getContent().stream().map(flight -> {
                    FlightResponseDto response = flightMapper.toResponseDto(flight);

                    try {
                        response.setAirline(airlineService.getAirlineById(flight.getAirlineId()));
                    } catch (Exception e) {
                        log.warn("Failed to load airline data for flight {}: {}", flight.getId(), e.getMessage());
                        // Set minimal airline info as fallback
                        response.setAirline(null);
                    }

                    try {
                        response.setAircraft(aircraftService.getAircraftById(flight.getAircraftId()));
                    } catch (Exception e) {
                        log.warn("Failed to load aircraft data for flight {}: {}", flight.getId(), e.getMessage());
                        // Set minimal aircraft info as fallback
                        response.setAircraft(null);
                    }

                    return response;
                }).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}