package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.ConflictType;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConflictDetectionService {

    private final OperationalFlightRepository flightRepository;

    public List<Conflict> detectConflicts(OperationalFlightCreateRequestDto request) {
        List<Conflict> conflicts = new ArrayList<>();

        // Flight number duplicate check
        if (flightRepository.existsByFlightNumberAndAirlineIdAndFlightDate(
                request.getFlightNumber(), request.getAirlineId(), request.getFlightDate())) {
            conflicts.add(new Conflict(ConflictType.FLIGHT_NUMBER_DUPLICATE,
                    "Flight number already exists for this airline on this date"));
        }

        // Aircraft double booking check
        List<OperationalFlight> aircraftFlights = flightRepository
                .findByAircraftIdAndFlightDate(request.getAircraftId(), request.getFlightDate());

        for (OperationalFlight existingFlight : aircraftFlights) {
            if (isTimeOverlap(request, existingFlight)) {
                conflicts.add(new Conflict(ConflictType.AIRCRAFT_DOUBLE_BOOKING,
                        String.format("Aircraft already booked for flight %s", existingFlight.getFlightNumber())));
            }
        }

        // Airport slot conflicts
        conflicts.addAll(checkAirportSlotConflicts(request));

        return conflicts;
    }

    private boolean isTimeOverlap(OperationalFlightCreateRequestDto request, OperationalFlight existing) {
        LocalTime requestDep = request.getScheduledDepartureTime();
        LocalTime requestArr = request.getScheduledArrivalTime();
        LocalTime existingDep = existing.getScheduledDepartureTime();
        LocalTime existingArr = existing.getScheduledArrivalTime();

        // Add buffer time (30 minutes) for aircraft turnaround
        LocalTime bufferedExistingArr = existingArr.plusMinutes(30);
        LocalTime bufferedRequestArr = requestArr.plusMinutes(30);

        return !(requestDep.isAfter(bufferedExistingArr) || existingDep.isAfter(bufferedRequestArr));
    }

    private List<Conflict> checkAirportSlotConflicts(OperationalFlightCreateRequestDto request) {
        List<Conflict> conflicts = new ArrayList<>();

        // Check origin airport slot (departure)
        List<OperationalFlight> originFlights = flightRepository
                .findByOriginStationIdAndFlightDate(request.getOriginStationId(), request.getFlightDate());

        for (OperationalFlight flight : originFlights) {
            if (isSlotConflict(request.getScheduledDepartureTime(), flight.getScheduledDepartureTime())) {
                conflicts.add(new Conflict(ConflictType.SLOT_CONFLICT,
                        String.format("Departure slot conflict with flight %s", flight.getFlightNumber())));
            }
        }

        return conflicts;
    }

    private boolean isSlotConflict(LocalTime time1, LocalTime time2) {
        // 30-minute slot window
        return Math.abs(Duration.between(time1, time2).toMinutes()) < 30;
    }

    public static class Conflict {
        private final ConflictType type;
        private final String description;

        public Conflict(ConflictType type, String description) {
            this.type = type;
            this.description = description;
        }

        public ConflictType getType() { return type; }
        public String getDescription() { return description; }
    }
}