package com.flightmanagement.flight.entity;

import com.flightmanagement.flight.enums.UploadStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_upload_batches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FlightUploadBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows;

    @Column(name = "successful_rows")
    @Builder.Default
    private Integer successfulRows = 0;

    @Column(name = "failed_rows")
    @Builder.Default
    private Integer failedRows = 0;

    @Column(name = "conflict_rows")
    @Builder.Default
    private Integer conflictRows = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UploadStatus status = UploadStatus.UPLOADED;

    @Column(name = "uploaded_by", nullable = false, length = 50)
    private String uploadedBy;

    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @Column(name = "processing_start_time")
    private LocalDateTime processingStartTime;

    @Column(name = "processing_end_time")
    private LocalDateTime processingEndTime;

    @Type(JsonBinaryType.class)
    @Column(name = "error_summary", columnDefinition = "json")
    private String errorSummary;

    @Type(JsonBinaryType.class)
    @Column(name = "conflict_summary", columnDefinition = "json")
    private String conflictSummary;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}