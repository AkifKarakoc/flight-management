package com.flightmanagement.reference.repository;

import com.flightmanagement.reference.entity.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    Page<Flight> findByAirlineId(Long airlineId, Pageable pageable);

    Page<Flight> findByFlightDate(LocalDate flightDate, Pageable pageable);

    Page<Flight> findByAirlineIdAndFlightDate(Long airlineId, LocalDate flightDate, Pageable pageable);

    boolean existsByFlightNumberAndAirlineIdAndFlightDate(String flightNumber, Long airlineId, LocalDate flightDate);
}