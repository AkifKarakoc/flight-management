package com.flightmanagement.reference.repository;

import com.flightmanagement.reference.entity.Aircraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {

    Page<Aircraft> findByManufacturerContainingIgnoreCase(String manufacturer, Pageable pageable);

    Page<Aircraft> findByTypeContainingIgnoreCase(String type, Pageable pageable);

    Page<Aircraft> findByIsActive(Boolean isActive, Pageable pageable);
}