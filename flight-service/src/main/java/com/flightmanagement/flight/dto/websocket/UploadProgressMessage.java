package com.flightmanagement.flight.dto.websocket;

import com.flightmanagement.flight.enums.UploadStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadProgressMessage {

    private Long batchId;
    private Integer totalRows;
    private Integer processedRows;
    private Integer successfulRows;
    private Integer failedRows;
    private Integer conflictRows;
    private UploadStatus status;
    private Double progressPercentage;
}