package com.flightmanagement.reference.repository;

import com.flightmanagement.reference.entity.RouteSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {

    List<RouteSegment> findByFlightIdOrderBySegmentOrder(Long flightId);

    boolean existsByFlightIdAndSegmentOrder(Long flightId, Integer segmentOrder);

    void deleteByFlightId(Long flightId);
}