package com.flightmanagement.flight.entity;

import com.flightmanagement.flight.enums.ConflictType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_conflicts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FlightConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_batch_id", nullable = false)
    private Long uploadBatchId;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_type", nullable = false)
    private ConflictType conflictType;

    @Column(name = "conflict_description", nullable = false, length = 500)
    private String conflictDescription;

    @Column(name = "existing_flight_id")
    private Long existingFlightId;

    @Type(JsonBinaryType.class)
    @Column(name = "new_flight_data", nullable = false, columnDefinition = "json")
    private String newFlightData;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}