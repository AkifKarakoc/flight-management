package com.flightmanagement.flight.repository;

import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.FlightStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OperationalFlightRepository extends JpaRepository<OperationalFlight, Long> {

    Page<OperationalFlight> findByAirlineId(Long airlineId, Pageable pageable);

    Page<OperationalFlight> findByFlightDate(LocalDate flightDate, Pageable pageable);

    Page<OperationalFlight> findByAirlineIdAndFlightDate(Long airlineId, LocalDate flightDate, Pageable pageable);

    @Query("SELECT f FROM OperationalFlight f WHERE f.flightNumber LIKE %:flightNumber% AND f.airlineId = :airlineId")
    Page<OperationalFlight> findByFlightNumberContainingAndAirlineId(String flightNumber, Long airlineId, Pageable pageable);

    boolean existsByFlightNumberAndAirlineIdAndFlightDate(String flightNumber, Long airlineId, LocalDate flightDate);

    List<OperationalFlight> findByAircraftIdAndFlightDate(Long aircraftId, LocalDate flightDate);

    List<OperationalFlight> findByOriginStationIdAndFlightDate(Long originStationId, LocalDate flightDate);

    List<OperationalFlight> findByDestinationStationIdAndFlightDate(Long destinationStationId, LocalDate flightDate);

    @Query("SELECT COUNT(f) FROM OperationalFlight f WHERE f.status = :status AND f.flightDate = :date")
    long countByStatusAndFlightDate(FlightStatus status, LocalDate date);

    @Query("SELECT f FROM OperationalFlight f WHERE f.flightDate = CURRENT_DATE AND f.isActive = true")
    List<OperationalFlight> findTodayActiveFlights();
}