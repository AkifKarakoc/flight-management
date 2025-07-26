package com.flightmanagement.flight.entity;

import com.flightmanagement.flight.enums.ChangeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_versions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"operational_flight_id", "version_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operational_flight_id", nullable = false)
    private Long operationalFlightId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @Column(name = "change_description", length = 500)
    private String changeDescription;

    @Column(name = "previous_data", columnDefinition = "JSON")
    private String previousData;

    @Column(name = "current_data", nullable = false, columnDefinition = "JSON")
    private String currentData;

    @Column(name = "changed_fields")
    private String changedFields;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;
}