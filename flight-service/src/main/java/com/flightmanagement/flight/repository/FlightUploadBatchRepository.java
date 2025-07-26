package com.flightmanagement.flight.repository;

import com.flightmanagement.flight.entity.FlightUploadBatch;
import com.flightmanagement.flight.enums.UploadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightUploadBatchRepository extends JpaRepository<FlightUploadBatch, Long> {

    Page<FlightUploadBatch> findByUploadedBy(String uploadedBy, Pageable pageable);

    Page<FlightUploadBatch> findByAirlineId(Long airlineId, Pageable pageable);

    Page<FlightUploadBatch> findByStatus(UploadStatus status, Pageable pageable);

    Page<FlightUploadBatch> findByAirlineIdAndUploadedBy(Long airlineId, String uploadedBy, Pageable pageable);
}