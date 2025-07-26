package com.flightmanagement.flight.repository;

import com.flightmanagement.flight.entity.FlightVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightVersionRepository extends JpaRepository<FlightVersion, Long> {

    List<FlightVersion> findByOperationalFlightIdOrderByVersionNumberDesc(Long operationalFlightId);

    FlightVersion findByOperationalFlightIdAndVersionNumber(Long operationalFlightId, Integer versionNumber);
}