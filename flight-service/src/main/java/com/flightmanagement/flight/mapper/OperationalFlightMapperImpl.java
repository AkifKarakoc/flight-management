package com.flightmanagement.flight.mapper;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.service.FlightEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationalFlightMapperImpl {

    private final FlightEnrichmentService enrichmentService;

    public OperationalFlight toEntity(OperationalFlightCreateRequestDto dto) {
        if (dto == null) {
            return null;
        }

        OperationalFlight flight = OperationalFlight.builder()
                .flightNumber(dto.getFlightNumber())
                .airlineId(dto.getAirlineId())
                .aircraftId(dto.getAircraftId())
                .flightDate(dto.getFlightDate())
                .scheduledDepartureTime(dto.getScheduledDepartureTime())
                .scheduledArrivalTime(dto.getScheduledArrivalTime())
                .originStationId(dto.getOriginStationId())
                .destinationStationId(dto.getDestinationStationId())
                .gate(dto.getGate())
                .terminal(dto.getTerminal())
                .flightType(dto.getFlightType())
                .build();

        // Enrich with reference data
        enrichmentService.enrichOperationalFlight(flight, dto);

        return flight;
    }

    public OperationalFlightResponseDto toResponseDto(OperationalFlight flight) {
        if (flight == null) {
            return null;
        }

        OperationalFlightResponseDto dto = new OperationalFlightResponseDto();
        dto.setId(flight.getId());
        dto.setFlightNumber(flight.getFlightNumber());
        dto.setAirlineCode(flight.getAirlineCode());
        dto.setAirlineName(flight.getAirlineName());
        dto.setAircraftType(flight.getAircraftType());
        dto.setFlightDate(flight.getFlightDate());
        dto.setScheduledDepartureTime(flight.getScheduledDepartureTime());
        dto.setScheduledArrivalTime(flight.getScheduledArrivalTime());
        dto.setActualDepartureTime(flight.getActualDepartureTime());
        dto.setActualArrivalTime(flight.getActualArrivalTime());
        dto.setDepartureDelay(flight.getDepartureDelay());
        dto.setArrivalDelay(flight.getArrivalDelay());
        dto.setOriginIcaoCode(flight.getOriginIcaoCode());
        dto.setDestinationIcaoCode(flight.getDestinationIcaoCode());
        dto.setGate(flight.getGate());
        dto.setTerminal(flight.getTerminal());
        dto.setStatus(flight.getStatus());
        dto.setFlightType(flight.getFlightType());
        dto.setVersion(flight.getVersion());
        dto.setIsActive(flight.getIsActive());
        dto.setCreatedAt(flight.getCreatedAt());
        dto.setUpdatedAt(flight.getUpdatedAt());

        return dto;
    }

    public void updateEntityFromDto(OperationalFlightCreateRequestDto dto, OperationalFlight flight) {
        if (dto == null || flight == null) {
            return;
        }

        flight.setFlightNumber(dto.getFlightNumber());
        flight.setAirlineId(dto.getAirlineId());
        flight.setAircraftId(dto.getAircraftId());
        flight.setFlightDate(dto.getFlightDate());
        flight.setScheduledDepartureTime(dto.getScheduledDepartureTime());
        flight.setScheduledArrivalTime(dto.getScheduledArrivalTime());
        flight.setOriginStationId(dto.getOriginStationId());
        flight.setDestinationStationId(dto.getDestinationStationId());
        flight.setGate(dto.getGate());
        flight.setTerminal(dto.getTerminal());
        flight.setFlightType(dto.getFlightType());

        // Re-enrich with updated reference data
        enrichmentService.enrichOperationalFlight(flight, dto);
    }
}