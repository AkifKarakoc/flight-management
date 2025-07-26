package com.flightmanagement.reference.repository;

import com.flightmanagement.reference.entity.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByIcaoCode(String icaoCode);

    Optional<Station> findByIataCode(String iataCode);

    @Query("SELECT s FROM Station s WHERE s.icaoCode LIKE %:query% OR s.iataCode LIKE %:query% OR s.name LIKE %:query%")
    List<Station> searchStations(String query);

    Page<Station> findByCountryContainingIgnoreCase(String country, Pageable pageable);

    Page<Station> findByCityContainingIgnoreCase(String city, Pageable pageable);
}