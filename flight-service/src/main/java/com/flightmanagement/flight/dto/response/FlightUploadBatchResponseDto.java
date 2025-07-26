package com.flightmanagement.flight.dto.response;

import com.flightmanagement.flight.enums.UploadStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlightUploadBatchResponseDto {

    private Long id;
    private String fileName;
    private Long fileSize;
    private Integer totalRows;
    private Integer successfulRows;
    private Integer failedRows;
    private Integer conflictRows;
    private UploadStatus status;
    private String uploadedBy;
    private Long airlineId;
    private String airlineName;
    private LocalDateTime processingStartTime;
    private LocalDateTime processingEndTime;
    private LocalDateTime createdAt;
}