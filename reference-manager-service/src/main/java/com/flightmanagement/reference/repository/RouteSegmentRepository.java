package com.flightmanagement.reference.repository;

import com.flightmanagement.reference.entity.RouteSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {

    List<RouteSegment> findByFlightIdOrderBySegmentOrder(Long flightId);

    boolean existsByFlightIdAndSegmentOrder(Long flightId, Integer segmentOrder);

    @Modifying
    @Query("DELETE FROM RouteSegment r WHERE r.flight.id = :flightId")
    void deleteByFlightId(@Param("flightId") Long flightId);
}