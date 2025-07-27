package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.websocket.FlightStatusUpdateMessage;
import com.flightmanagement.flight.dto.websocket.UploadProgressMessage;
import com.flightmanagement.flight.entity.FlightUploadBatch;
import com.flightmanagement.flight.entity.OperationalFlight;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyFlightCreated(OperationalFlight flight) {
        try {
            FlightStatusUpdateMessage message = FlightStatusUpdateMessage.builder()
                    .flightId(flight.getId())
                    .flightNumber(flight.getFlightNumber())
                    .previousStatus(null)
                    .currentStatus(flight.getStatus())
                    .timestamp(flight.getCreatedAt())
                    .build();

            messagingTemplate.convertAndSend("/topic/flights/" + flight.getId(), message);
            messagingTemplate.convertAndSend("/topic/airlines/" + flight.getAirlineId() + "/flights", message);
            messagingTemplate.convertAndSend("/topic/dashboard", message);

            log.debug("Sent flight created notification for: {}", flight.getFlightNumber());
        } catch (Exception e) {
            log.error("Failed to send flight created notification", e);
        }
    }

    public void notifyFlightUpdated(OperationalFlight flight, OperationalFlight previousState) {
        try {
            FlightStatusUpdateMessage message = FlightStatusUpdateMessage.builder()
                    .flightId(flight.getId())
                    .flightNumber(flight.getFlightNumber())
                    .previousStatus(previousState.getStatus())
                    .currentStatus(flight.getStatus())
                    .timestamp(flight.getUpdatedAt())
                    .build();

            messagingTemplate.convertAndSend("/topic/flights/" + flight.getId(), message);
            messagingTemplate.convertAndSend("/topic/airlines/" + flight.getAirlineId() + "/flights", message);
            messagingTemplate.convertAndSend("/topic/dashboard", message);

            log.debug("Sent flight updated notification for: {}", flight.getFlightNumber());
        } catch (Exception e) {
            log.error("Failed to send flight updated notification", e);
        }
    }

    public void notifyFlightStatusUpdate(OperationalFlight flight) {
        try {
            FlightStatusUpdateMessage message = FlightStatusUpdateMessage.builder()
                    .flightId(flight.getId())
                    .flightNumber(flight.getFlightNumber())
                    .currentStatus(flight.getStatus())
                    .actualTime(flight.getActualDepartureTime() != null ? 
                              flight.getActualDepartureTime() : flight.getActualArrivalTime())
                    .delay(flight.getDepartureDelay() != null ? 
                          flight.getDepartureDelay() : flight.getArrivalDelay())
                    .timestamp(flight.getUpdatedAt())
                    .build();

            messagingTemplate.convertAndSend("/topic/flights/" + flight.getId(), message);
            messagingTemplate.convertAndSend("/topic/airlines/" + flight.getAirlineId() + "/flights", message);
            messagingTemplate.convertAndSend("/topic/dashboard", message);

            log.debug("Sent flight status update notification for: {}", flight.getFlightNumber());
        } catch (Exception e) {
            log.error("Failed to send flight status update notification", e);
        }
    }

    public void notifyUploadProgress(FlightUploadBatch batch) {
        try {
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

            log.debug("Sent upload progress notification for batch: {}", batch.getId());
        } catch (Exception e) {
            log.error("Failed to send upload progress notification", e);
        }
    }

    public void notifyUploadCompleted(FlightUploadBatch batch) {
        try {
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

            log.debug("Sent upload completed notification for batch: {}", batch.getId());
        } catch (Exception e) {
            log.error("Failed to send upload completed notification", e);
        }
    }

    private double calculateProgress(FlightUploadBatch batch) {
        if (batch.getTotalRows() == 0) {
            return 0.0;
        }
        int processed = batch.getSuccessfulRows() + batch.getFailedRows() + batch.getConflictRows();
        return (double) processed / batch.getTotalRows() * 100.0;
    }
}