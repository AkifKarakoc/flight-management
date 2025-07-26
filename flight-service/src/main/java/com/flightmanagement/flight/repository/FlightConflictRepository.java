package com.flightmanagement.flight.repository;

import com.flightmanagement.flight.entity.FlightConflict;
import com.flightmanagement.flight.enums.ConflictType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightConflictRepository extends JpaRepository<FlightConflict, Long> {

    List<FlightConflict> findByUploadBatchId(Long uploadBatchId);

    Page<FlightConflict> findByConflictType(ConflictType conflictType, Pageable pageable);

    long countByUploadBatchIdAndResolutionIsNull(Long uploadBatchId);
}