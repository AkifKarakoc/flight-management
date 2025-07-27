package com.flightmanagement.flight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.entity.FlightVersion;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.ChangeType;
import com.flightmanagement.flight.repository.FlightVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightVersionService {

    private final FlightVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public void createInitialVersion(OperationalFlight flight) {
        try {
            FlightVersion version = FlightVersion.builder()
                    .operationalFlightId(flight.getId())
                    .versionNumber(1)
                    .changeType(ChangeType.SCHEDULE_CHANGE)
                    .changeDescription("Initial flight creation")
                    .currentData(objectMapper.writeValueAsString(mapFlightToJson(flight)))
                    .changedFields("all")
                    .createdAt(LocalDateTime.now())
                    .createdBy(flight.getCreatedBy())
                    .build();

            versionRepository.save(version);
            log.debug("Created initial version for flight: {}", flight.getFlightNumber());
        } catch (Exception e) {
            log.error("Failed to create initial version for flight: {}", flight.getId(), e);
        }
    }

    public void createVersionEntry(OperationalFlight flight, OperationalFlight previousState,
                                 OperationalFlightCreateRequestDto request, boolean isMajorChange) {
        try {
            ChangeType changeType = determineChangeType(previousState, request);
            String changeDescription = generateChangeDescription(changeType, previousState, request);
            String changedFields = identifyChangedFields(previousState, request);

            FlightVersion version = FlightVersion.builder()
                    .operationalFlightId(flight.getId())
                    .versionNumber(flight.getVersion())
                    .changeType(changeType)
                    .changeDescription(changeDescription)
                    .previousData(objectMapper.writeValueAsString(mapFlightToJson(previousState)))
                    .currentData(objectMapper.writeValueAsString(mapFlightToJson(flight)))
                    .changedFields(changedFields)
                    .createdAt(LocalDateTime.now())
                    .createdBy(flight.getUpdatedBy())
                    .build();

            versionRepository.save(version);
            log.debug("Created version {} for flight: {} with change type: {}", 
                     flight.getVersion(), flight.getFlightNumber(), changeType);
        } catch (Exception e) {
            log.error("Failed to create version entry for flight: {}", flight.getId(), e);
        }
    }

    public boolean isMajorChange(OperationalFlight currentFlight, OperationalFlightCreateRequestDto request) {
        // Check for major changes that require new version
        if (currentFlight.getScheduledDepartureTime() != null && request.getScheduledDepartureTime() != null) {
            long departureDiff = Math.abs(
                currentFlight.getScheduledDepartureTime().toSecondOfDay() - 
                request.getScheduledDepartureTime().toSecondOfDay()
            );
            if (departureDiff > 1800) { // 30 minutes
                return true;
            }
        }

        if (currentFlight.getScheduledArrivalTime() != null && request.getScheduledArrivalTime() != null) {
            long arrivalDiff = Math.abs(
                currentFlight.getScheduledArrivalTime().toSecondOfDay() - 
                request.getScheduledArrivalTime().toSecondOfDay()
            );
            if (arrivalDiff > 1800) { // 30 minutes
                return true;
            }
        }

        // Aircraft change is always major
        if (!currentFlight.getAircraftId().equals(request.getAircraftId())) {
            return true;
        }

        return false;
    }

    private ChangeType determineChangeType(OperationalFlight previousState, OperationalFlightCreateRequestDto request) {
        if (!previousState.getAircraftId().equals(request.getAircraftId())) {
            return ChangeType.AIRCRAFT_CHANGE;
        }

        if (hasTimeChange(previousState, request)) {
            return ChangeType.TIME_CHANGE;
        }

        return ChangeType.SCHEDULE_CHANGE;
    }

    private boolean hasTimeChange(OperationalFlight previousState, OperationalFlightCreateRequestDto request) {
        if (previousState.getScheduledDepartureTime() != null && request.getScheduledDepartureTime() != null) {
            if (!previousState.getScheduledDepartureTime().equals(request.getScheduledDepartureTime())) {
                return true;
            }
        }

        if (previousState.getScheduledArrivalTime() != null && request.getScheduledArrivalTime() != null) {
            if (!previousState.getScheduledArrivalTime().equals(request.getScheduledArrivalTime())) {
                return true;
            }
        }

        return false;
    }

    private String generateChangeDescription(ChangeType changeType, OperationalFlight previousState, 
                                          OperationalFlightCreateRequestDto request) {
        switch (changeType) {
            case AIRCRAFT_CHANGE:
                return "Aircraft changed from " + previousState.getAircraftType() + " to " + 
                       (request.getAircraftId() != null ? "new aircraft" : "unknown");
            case TIME_CHANGE:
                return "Flight times updated";
            case STATUS_UPDATE:
                return "Flight status updated";
            default:
                return "Flight schedule updated";
        }
    }

    private String identifyChangedFields(OperationalFlight previousState, OperationalFlightCreateRequestDto request) {
        StringBuilder changedFields = new StringBuilder();

        if (request.getScheduledDepartureTime() != null && 
            !request.getScheduledDepartureTime().equals(previousState.getScheduledDepartureTime())) {
            changedFields.append("scheduledDepartureTime,");
        }

        if (request.getScheduledArrivalTime() != null && 
            !request.getScheduledArrivalTime().equals(previousState.getScheduledArrivalTime())) {
            changedFields.append("scheduledArrivalTime,");
        }

        if (request.getAircraftId() != null && 
            !request.getAircraftId().equals(previousState.getAircraftId())) {
            changedFields.append("aircraftId,");
        }

        if (request.getGate() != null && 
            !request.getGate().equals(previousState.getGate())) {
            changedFields.append("gate,");
        }

        if (request.getTerminal() != null && 
            !request.getTerminal().equals(previousState.getTerminal())) {
            changedFields.append("terminal,");
        }

        // Remove trailing comma
        if (changedFields.length() > 0) {
            changedFields.setLength(changedFields.length() - 1);
        }

        return changedFields.toString();
    }

    private Map<String, Object> mapFlightToJson(OperationalFlight flight) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", flight.getId());
        data.put("flightNumber", flight.getFlightNumber());
        data.put("airlineId", flight.getAirlineId());
        data.put("airlineCode", flight.getAirlineCode());
        data.put("airlineName", flight.getAirlineName());
        data.put("aircraftId", flight.getAircraftId());
        data.put("aircraftType", flight.getAircraftType());
        data.put("flightDate", flight.getFlightDate());
        data.put("scheduledDepartureTime", flight.getScheduledDepartureTime());
        data.put("scheduledArrivalTime", flight.getScheduledArrivalTime());
        data.put("actualDepartureTime", flight.getActualDepartureTime());
        data.put("actualArrivalTime", flight.getActualArrivalTime());
        data.put("departureDelay", flight.getDepartureDelay());
        data.put("arrivalDelay", flight.getArrivalDelay());
        data.put("originStationId", flight.getOriginStationId());
        data.put("originIcaoCode", flight.getOriginIcaoCode());
        data.put("destinationStationId", flight.getDestinationStationId());
        data.put("destinationIcaoCode", flight.getDestinationIcaoCode());
        data.put("gate", flight.getGate());
        data.put("terminal", flight.getTerminal());
        data.put("status", flight.getStatus());
        data.put("flightType", flight.getFlightType());
        data.put("cancellationReason", flight.getCancellationReason());
        data.put("delayReason", flight.getDelayReason());
        data.put("version", flight.getVersion());
        data.put("uploadBatchId", flight.getUploadBatchId());
        data.put("isActive", flight.getIsActive());
        data.put("createdAt", flight.getCreatedAt());
        data.put("updatedAt", flight.getUpdatedAt());
        data.put("createdBy", flight.getCreatedBy());
        data.put("updatedBy", flight.getUpdatedBy());
        return data;
    }
}