package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.entity.FlightVersion;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.ChangeType;
import com.flightmanagement.flight.repository.FlightVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FlightVersionService {

    private final FlightVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public void createInitialVersion(OperationalFlight flight) {
        try {
            FlightVersion version = FlightVersion.builder()
                    .operationalFlightId(flight.getId())
                    .versionNumber(1)
                    .changeType(ChangeType.CREATED)
                    .changeDescription("Initial flight creation")
                    .previousData(null)
                    .currentData(objectMapper.writeValueAsString(flight))
                    .changedFields("ALL")
                    .createdBy(flight.getCreatedBy())
                    .createdAt(LocalDateTime.now())
                    .build();

            versionRepository.save(version);
            log.debug("Created initial version for flight: {}", flight.getFlightNumber());

        } catch (Exception e) {
            log.error("Failed to create initial version for flight: {}", flight.getId(), e);
        }
    }

    public boolean isMajorChange(OperationalFlight existing, OperationalFlightCreateRequestDto request) {
        // Aircraft change is major
        if (!existing.getAircraftId().equals(request.getAircraftId())) {
            return true;
        }

        // Time change > 30 minutes is major
        Duration depChange = Duration.between(existing.getScheduledDepartureTime(), request.getScheduledDepartureTime());
        Duration arrChange = Duration.between(existing.getScheduledArrivalTime(), request.getScheduledArrivalTime());

        if (Math.abs(depChange.toMinutes()) > 30 || Math.abs(arrChange.toMinutes()) > 30) {
            return true;
        }

        // Station change is major
        if (!existing.getOriginStationId().equals(request.getOriginStationId()) ||
                !existing.getDestinationStationId().equals(request.getDestinationStationId())) {
            return true;
        }

        return false;
    }

    public void createVersionEntry(OperationalFlight current, OperationalFlight previous,
                                   OperationalFlightCreateRequestDto request, boolean isMajorChange) {
        try {
            ChangeType changeType = determineChangeType(current, previous, isMajorChange);
            List<String> changedFields = getChangedFields(current, previous);

            FlightVersion version = FlightVersion.builder()
                    .operationalFlightId(current.getId())
                    .versionNumber(current.getVersion())
                    .changeType(changeType)
                    .changeDescription(generateChangeDescription(changeType, changedFields))
                    .previousData(objectMapper.writeValueAsString(previous))
                    .currentData(objectMapper.writeValueAsString(current))
                    .changedFields(String.join(",", changedFields))
                    .createdBy(current.getUpdatedBy())
                    .createdAt(LocalDateTime.now())
                    .build();

            versionRepository.save(version);
            log.debug("Created version {} for flight: {}", current.getVersion(), current.getFlightNumber());

        } catch (Exception e) {
            log.error("Failed to create version entry for flight: {}", current.getId(), e);
        }
    }

    private ChangeType determineChangeType(OperationalFlight current, OperationalFlight previous, boolean isMajorChange) {
        if (!current.getStatus().equals(previous.getStatus())) {
            return ChangeType.STATUS_UPDATE;
        }

        if (!current.getAircraftId().equals(previous.getAircraftId())) {
            return ChangeType.AIRCRAFT_CHANGE;
        }

        if (isMajorChange) {
            return ChangeType.SCHEDULE_CHANGE;
        }

        return ChangeType.TIME_CHANGE;
    }

    private List<String> getChangedFields(OperationalFlight current, OperationalFlight previous) {
        List<String> changes = new ArrayList<>();

        if (!current.getAircraftId().equals(previous.getAircraftId())) {
            changes.add("aircraftId");
        }
        if (!current.getScheduledDepartureTime().equals(previous.getScheduledDepartureTime())) {
            changes.add("scheduledDepartureTime");
        }
        if (!current.getScheduledArrivalTime().equals(previous.getScheduledArrivalTime())) {
            changes.add("scheduledArrivalTime");
        }
        if (!current.getStatus().equals(previous.getStatus())) {
            changes.add("status");
        }

        return changes;
    }

    private String generateChangeDescription(ChangeType changeType, List<String> changedFields) {
        return switch (changeType) {
            case AIRCRAFT_CHANGE -> "Aircraft changed";
            case SCHEDULE_CHANGE -> "Major schedule change: " + String.join(", ", changedFields);
            case TIME_CHANGE -> "Minor time adjustment";
            case STATUS_UPDATE -> "Flight status updated";
            case CREATED -> "Flight created";
            default -> "Flight updated";
        };
    }
}