package com.flightmanagement.reference.repository;

import com.flightmanagement.reference.entity.Airline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Long> {

    Optional<Airline> findByCode(String code);

    boolean existsByCode(String code);

    Page<Airline> findByIsActive(Boolean isActive, Pageable pageable);

    Page<Airline> findByCountryContainingIgnoreCase(String country, Pageable pageable);
}