package com.flightmanagement.flight.entity;

import com.flightmanagement.flight.enums.FlightStatus;
import com.flightmanagement.flight.enums.FlightType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "operational_flights",
        uniqueConstraints = @UniqueConstraint(columnNames = {"flight_number", "airline_id", "flight_date", "version"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OperationalFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_number", nullable = false, length = 10)
    private String flightNumber;

    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @Column(name = "airline_code", nullable = false, length = 3)
    private String airlineCode;

    @Column(name = "airline_name", nullable = false, length = 100)
    private String airlineName;

    @Column(name = "aircraft_id", nullable = false)
    private Long aircraftId;

    @Column(name = "aircraft_type", nullable = false, length = 20)
    private String aircraftType;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    @Column(name = "scheduled_departure_time", nullable = false)
    private LocalTime scheduledDepartureTime;

    @Column(name = "scheduled_arrival_time", nullable = false)
    private LocalTime scheduledArrivalTime;

    @Column(name = "actual_departure_time")
    private LocalTime actualDepartureTime;

    @Column(name = "actual_arrival_time")
    private LocalTime actualArrivalTime;

    @Column(name = "departure_delay")
    @Builder.Default
    private Integer departureDelay = 0;

    @Column(name = "arrival_delay")
    @Builder.Default
    private Integer arrivalDelay = 0;

    @Column(name = "origin_station_id", nullable = false)
    private Long originStationId;

    @Column(name = "origin_icao_code", nullable = false, length = 4)
    private String originIcaoCode;

    @Column(name = "destination_station_id", nullable = false)
    private Long destinationStationId;

    @Column(name = "destination_icao_code", nullable = false, length = 4)
    private String destinationIcaoCode;

    @Column(length = 10)
    private String gate;

    @Column(length = 5)
    private String terminal;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "flight_type", nullable = false)
    private FlightType flightType;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "delay_reason")
    private String delayReason;

    @Builder.Default
    private Integer version = 1;

    @Column(name = "upload_batch_id")
    private Long uploadBatchId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 50)
    private String updatedBy;
}