package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.websocket.FlightStatusUpdateMessage;
import com.flightmanagement.flight.dto.websocket.UploadProgressMessage;
import com.flightmanagement.flight.entity.FlightUploadBatch;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.FlightStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyFlightCreated(OperationalFlight flight) {
        FlightStatusUpdateMessage message = FlightStatusUpdateMessage.builder()
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .currentStatus(flight.getStatus())
                .eventType("FLIGHT_CREATED")
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/flights/" + flight.getId(), message);
        messagingTemplate.convertAndSend("/topic/airlines/" + flight.getAirlineId() + "/flights", message);

        log.debug("Sent WebSocket notification for flight created: {}", flight.getFlightNumber());
    }

    public void notifyFlightUpdated(OperationalFlight flight, OperationalFlight previousState) {
        FlightStatusUpdateMessage message = FlightStatusUpdateMessage.builder()
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .previousStatus(previousState.getStatus())
                .currentStatus(flight.getStatus())
                .eventType("FLIGHT_UPDATED")
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/flights/" + flight.getId(), message);
        messagingTemplate.convertAndSend("/topic/airlines/" + flight.getAirlineId() + "/flights", message);
    }

    public void notifyUploadProgress(FlightUploadBatch batch) {
        UploadProgressMessage message = UploadProgressMessage.builder()
                .batchId(batch.getId())
                .totalRows(batch.getTotalRows())
                .processedRows(batch.getSuccessfulRows() + batch.getFailedRows() + batch.getConflictRows())
                .successfulRows(batch.getSuccessfulRows())
                .failedRows(batch.getFailedRows())
                .conflictRows(batch.getConflictRows())
                .status(batch.getStatus())
                .progressPercentage(calculateProgress(batch))
                .build();

        messagingTemplate.convertAndSend("/topic/uploads/" + batch.getId(), message);
        messagingTemplate.convertAndSend("/topic/airlines/" + batch.getAirlineId() + "/uploads", message);
    }

    public void notifyUploadCompleted(FlightUploadBatch batch) {
        UploadProgressMessage message = UploadProgressMessage.builder()
                .batchId(batch.getId())
                .totalRows(batch.getTotalRows())
                .processedRows(batch.getTotalRows())
                .successfulRows(batch.getSuccessfulRows())
                .failedRows(batch.getFailedRows())
                .conflictRows(batch.getConflictRows())
                .status(batch.getStatus())
                .progressPercentage(100.0)
                .build();

        messagingTemplate.convertAndSend("/topic/uploads/" + batch.getId(), message);
        messagingTemplate.convertAndSend("/topic/airlines/" + batch.getAirlineId() + "/uploads", message);
    }

    public void notifyFlightStatusUpdate(OperationalFlight flight, FlightStatus previousStatus) {
        FlightStatusUpdateMessage message = FlightStatusUpdateMessage.builder()
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .previousStatus(previousStatus)
                .currentStatus(flight.getStatus())
                .actualTime(flight.getActualArrivalTime() != null ? flight.getActualArrivalTime() : flight.getActualDepartureTime())
                .delay(flight.getArrivalDelay() != null ? flight.getArrivalDelay() : flight.getDepartureDelay())
                .eventType("STATUS_UPDATE")
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/flights/" + flight.getId(), message);
        messagingTemplate.convertAndSend("/topic/airlines/" + flight.getAirlineId() + "/flights", message);

        log.debug("Sent WebSocket notification for flight status update: {}", flight.getFlightNumber());
    }

    private Double calculateProgress(FlightUploadBatch batch) {
        if (batch.getTotalRows() == 0) return 0.0;

        int processedRows = batch.getSuccessfulRows() + batch.getFailedRows() + batch.getConflictRows();
        return (double) processedRows / batch.getTotalRows() * 100;
    }
}