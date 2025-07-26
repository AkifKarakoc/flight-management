package com.flightmanagement.reference.dto.kafka;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReferenceEventDto {

    private String eventType;
    private String entityType;
    private Long entityId;
    private Long airlineId;
    private LocalDateTime timestamp;
    private Object data;
}