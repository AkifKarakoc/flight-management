package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightEnrichmentService {

    private final ReferenceDataService referenceDataService;

    public void enrichOperationalFlight(OperationalFlight flight, OperationalFlightCreateRequestDto request) {
        try {
            // Enrich airline data
            enrichAirlineData(flight, request.getAirlineId());

            // Enrich aircraft data
            enrichAircraftData(flight, request.getAircraftId());

            // Enrich station data
            enrichStationData(flight, request.getOriginStationId(), request.getDestinationStationId());

            log.debug("Successfully enriched flight data for: {}", flight.getFlightNumber());

        } catch (Exception e) {
            log.error("Failed to enrich flight data for: {}", flight.getFlightNumber(), e);
            // Set fallback values
            setFallbackData(flight, request);
        }
    }

    public void enrichFromCsvData(OperationalFlight flight, String airlineCode, String aircraftType,
                                  String originIcao, String destinationIcao) {
        try {
            // Resolve airline by code
            var airlineOpt = referenceDataService.getAirlineByCode(airlineCode);
            if (airlineOpt.isPresent()) {
                Map<String, Object> airline = airlineOpt.get();
                flight.setAirlineId(((Number) airline.get("id")).longValue());
                flight.setAirlineCode((String) airline.get("code"));
                flight.setAirlineName((String) airline.get("name"));
            } else {
                flight.setAirlineCode(airlineCode);
                flight.setAirlineName("Unknown Airline");
            }

            // Resolve aircraft by type
            var aircraftOpt = referenceDataService.getAircraftByType(aircraftType);
            if (aircraftOpt.isPresent()) {
                Map<String, Object> aircraft = aircraftOpt.get();
                flight.setAircraftId(((Number) aircraft.get("id")).longValue());
                flight.setAircraftType((String) aircraft.get("type"));
            } else {
                flight.setAircraftType(aircraftType);
            }

            // Resolve origin station
            var originOpt = referenceDataService.getStationByIcao(originIcao);
            if (originOpt.isPresent()) {
                Map<String, Object> origin = originOpt.get();
                flight.setOriginStationId(((Number) origin.get("id")).longValue());
                flight.setOriginIcaoCode((String) origin.get("icaoCode"));
            } else {
                flight.setOriginIcaoCode(originIcao);
            }

            // Resolve destination station
            var destinationOpt = referenceDataService.getStationByIcao(destinationIcao);
            if (destinationOpt.isPresent()) {
                Map<String, Object> destination = destinationOpt.get();
                flight.setDestinationStationId(((Number) destination.get("id")).longValue());
                flight.setDestinationIcaoCode((String) destination.get("icaoCode"));
            } else {
                flight.setDestinationIcaoCode(destinationIcao);
            }

            log.debug("Successfully enriched CSV flight data for: {}", flight.getFlightNumber());

        } catch (Exception e) {
            log.error("Failed to enrich CSV flight data for: {}", flight.getFlightNumber(), e);
            // Keep original values from CSV
            flight.setAirlineCode(airlineCode);
            flight.setAircraftType(aircraftType);
            flight.setOriginIcaoCode(originIcao);
            flight.setDestinationIcaoCode(destinationIcao);
        }
    }

    private void enrichAirlineData(OperationalFlight flight, Long airlineId) {
        try {
            Map<String, Object> airline = referenceDataService.getAirline(airlineId);
            if (airline != null) {
                flight.setAirlineCode((String) airline.get("code"));
                flight.setAirlineName((String) airline.get("name"));
            } else {
                setFallbackAirlineData(flight);
            }
        } catch (Exception e) {
            log.warn("Failed to enrich airline data for ID: {}", airlineId, e);
            setFallbackAirlineData(flight);
        }
    }

    private void enrichAircraftData(OperationalFlight flight, Long aircraftId) {
        try {
            Map<String, Object> aircraft = referenceDataService.getAircraft(aircraftId);
            if (aircraft != null) {
                flight.setAircraftType((String) aircraft.get("type"));
            } else {
                flight.setAircraftType("Unknown");
            }
        } catch (Exception e) {
            log.warn("Failed to enrich aircraft data for ID: {}", aircraftId, e);
            flight.setAircraftType("Unknown");
        }
    }

    private void enrichStationData(OperationalFlight flight, Long originStationId, Long destinationStationId) {
        // Enrich origin station
        try {
            Map<String, Object> originStation = referenceDataService.getStation(originStationId);
            if (originStation != null) {
                flight.setOriginIcaoCode((String) originStation.get("icaoCode"));
            } else {
                flight.setOriginIcaoCode("XXXX");
            }
        } catch (Exception e) {
            log.warn("Failed to enrich origin station data for ID: {}", originStationId, e);
            flight.setOriginIcaoCode("XXXX");
        }

        // Enrich destination station
        try {
            Map<String, Object> destinationStation = referenceDataService.getStation(destinationStationId);
            if (destinationStation != null) {
                flight.setDestinationIcaoCode((String) destinationStation.get("icaoCode"));
            } else {
                flight.setDestinationIcaoCode("YYYY");
            }
        } catch (Exception e) {
            log.warn("Failed to enrich destination station data for ID: {}", destinationStationId, e);
            flight.setDestinationIcaoCode("YYYY");
        }
    }

    private void setFallbackData(OperationalFlight flight, OperationalFlightCreateRequestDto request) {
        setFallbackAirlineData(flight);
        if (flight.getAircraftType() == null) {
            flight.setAircraftType("Unknown");
        }
        if (flight.getOriginIcaoCode() == null) {
            flight.setOriginIcaoCode("XXXX");
        }
        if (flight.getDestinationIcaoCode() == null) {
            flight.setDestinationIcaoCode("YYYY");
        }
    }

    private void setFallbackAirlineData(OperationalFlight flight) {
        if (flight.getAirlineCode() == null) {
            flight.setAirlineCode("XX");
        }
        if (flight.getAirlineName() == null) {
            flight.setAirlineName("Unknown Airline");
        }
    }
}