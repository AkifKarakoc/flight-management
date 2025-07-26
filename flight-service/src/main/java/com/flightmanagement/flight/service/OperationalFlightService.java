package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.dto.response.PagedResponse;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.exception.FlightConflictException;
import com.flightmanagement.flight.exception.FlightNotFoundException;
import com.flightmanagement.flight.exception.UnauthorizedFlightAccessException;
import com.flightmanagement.flight.mapper.OperationalFlightMapperImpl;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OperationalFlightService {

    private final OperationalFlightRepository flightRepository;
    private final OperationalFlightMapperImpl flightMapper;
    private final ConflictDetectionService conflictService;
    private final ReferenceDataService referenceService;
    private final EventPublishService eventPublishService;
    private final WebSocketService webSocketService;
    private final FlightVersionService versionService;

    public PagedResponse<OperationalFlightResponseDto> getFlights(Pageable pageable, UserContext userContext) {
        Page<OperationalFlight> flights;

        if (userContext.isAirlineUser()) {
            flights = flightRepository.findByAirlineId(userContext.getAirlineId(), pageable);
        } else {
            flights = flightRepository.findAll(pageable);
        }

        return createPagedResponse(flights);
    }

    public OperationalFlightResponseDto getFlightById(Long id, UserContext userContext) {
        OperationalFlight flight = getFlightWithAccessCheck(id, userContext);
        return flightMapper.toResponseDto(flight);
    }

    public OperationalFlightResponseDto createFlight(OperationalFlightCreateRequestDto request, UserContext userContext) {
        // Validate user can create flight for airline
        validateAirlineAccess(request.getAirlineId(), userContext);

        // Validate reference data and enrich
        enrichFlightWithReferenceData(request);

        // Conflict detection
        List<ConflictDetectionService.Conflict> conflicts = conflictService.detectConflicts(request);
        if (!conflicts.isEmpty()) {
            throw new FlightConflictException("Conflicts detected", conflicts);
        }

        // Create operational flight
        OperationalFlight flight = flightMapper.toEntity(request);
        flight.setCreatedBy(userContext.getUsername());
        flight.setUpdatedBy(userContext.getUsername());
        flight.setVersion(1);

        flight = flightRepository.save(flight);

        // Create initial version
        versionService.createInitialVersion(flight);

        // Publish Kafka event
        eventPublishService.publishFlightEvent("FLIGHT_CREATED", flight, userContext);

        // WebSocket notification
        webSocketService.notifyFlightCreated(flight);

        log.info("Created flight: {} for airline: {}", flight.getFlightNumber(), flight.getAirlineCode());
        return flightMapper.toResponseDto(flight);
    }

    public OperationalFlightResponseDto updateFlight(Long id, OperationalFlightCreateRequestDto request, UserContext userContext) {
        OperationalFlight existingFlight = getFlightWithAccessCheck(id, userContext);

        // Store previous state for versioning and events
        OperationalFlight previousState = cloneFlight(existingFlight);

        // Determine if major change (new version needed)
        boolean isMajorChange = versionService.isMajorChange(existingFlight, request);

        // Enrich with reference data
        enrichFlightWithReferenceData(request);

        // Update flight
        flightMapper.updateEntityFromDto(request, existingFlight);
        existingFlight.setUpdatedBy(userContext.getUsername());

        if (isMajorChange) {
            existingFlight.setVersion(existingFlight.getVersion() + 1);
        }

        existingFlight = flightRepository.save(existingFlight);

        // Create version entry
        versionService.createVersionEntry(existingFlight, previousState, request, isMajorChange);

        // Publish events
        eventPublishService.publishFlightEvent("FLIGHT_UPDATED", existingFlight, userContext);

        // WebSocket notification
        webSocketService.notifyFlightUpdated(existingFlight, previousState);

        log.info("Updated flight: {} version: {}", existingFlight.getFlightNumber(), existingFlight.getVersion());
        return flightMapper.toResponseDto(existingFlight);
    }

    public void deleteFlight(Long id, UserContext userContext) {
        OperationalFlight flight = getFlightWithAccessCheck(id, userContext);

        flight.setIsActive(false);
        flight.setUpdatedBy(userContext.getUsername());
        flightRepository.save(flight);

        eventPublishService.publishFlightEvent("FLIGHT_DELETED", flight, userContext);
        log.info("Deleted flight: {}", flight.getFlightNumber());
    }

    private void validateAirlineAccess(Long airlineId, UserContext userContext) {
        if (userContext.isAirlineUser() && !airlineId.equals(userContext.getAirlineId())) {
            throw new UnauthorizedFlightAccessException("Cannot create flight for different airline");
        }
    }

    private OperationalFlight getFlightWithAccessCheck(Long id, UserContext userContext) {
        OperationalFlight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found with id: " + id));

        if (userContext.isAirlineUser() && !flight.getAirlineId().equals(userContext.getAirlineId())) {
            throw new UnauthorizedFlightAccessException("Cannot access flights from different airline");
        }

        return flight;
    }

    private void enrichFlightWithReferenceData(OperationalFlightCreateRequestDto request) {
        try {
            // Fetch and cache airline data
            Map<String, Object> airline = referenceService.getAirline(request.getAirlineId());
            if (airline != null) {
                // Airline data will be used in mapper
                log.debug("Enriched airline data for ID: {}", request.getAirlineId());
            }

            // Fetch and cache aircraft data
            Map<String, Object> aircraft = referenceService.getAircraft(request.getAircraftId());
            if (aircraft != null) {
                log.debug("Enriched aircraft data for ID: {}", request.getAircraftId());
            }

            // Fetch and cache station data
            Map<String, Object> originStation = referenceService.getStation(request.getOriginStationId());
            Map<String, Object> destinationStation = referenceService.getStation(request.getDestinationStationId());

            if (originStation != null && destinationStation != null) {
                log.debug("Enriched station data for origin: {} and destination: {}",
                        request.getOriginStationId(), request.getDestinationStationId());
            }

        } catch (Exception e) {
            log.warn("Failed to enrich flight with reference data: {}", e.getMessage());
            // Continue with flight creation even if reference data enrichment fails
        }
    }

    private OperationalFlight cloneFlight(OperationalFlight original) {
        return OperationalFlight.builder()
                .id(original.getId())
                .flightNumber(original.getFlightNumber())
                .airlineId(original.getAirlineId())
                .aircraftId(original.getAircraftId())
                .flightDate(original.getFlightDate())
                .scheduledDepartureTime(original.getScheduledDepartureTime())
                .scheduledArrivalTime(original.getScheduledArrivalTime())
                .status(original.getStatus())
                .version(original.getVersion())
                .build();
    }

    private PagedResponse<OperationalFlightResponseDto> createPagedResponse(Page<OperationalFlight> page) {
        return PagedResponse.<OperationalFlightResponseDto>builder()
                .content(page.getContent().stream().map(flightMapper::toResponseDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}