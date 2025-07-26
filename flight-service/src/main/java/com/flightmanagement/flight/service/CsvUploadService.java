package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.dto.response.FlightUploadBatchResponseDto;
import com.flightmanagement.flight.entity.FlightConflict;
import com.flightmanagement.flight.entity.FlightUploadBatch;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.ConflictType;
import com.flightmanagement.flight.enums.FlightType;
import com.flightmanagement.flight.enums.UploadStatus;
import com.flightmanagement.flight.exception.CsvProcessingException;
import com.flightmanagement.flight.mapper.OperationalFlightMapper;
import com.flightmanagement.flight.repository.FlightConflictRepository;
import com.flightmanagement.flight.repository.FlightUploadBatchRepository;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.security.UserContext;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvUploadService {

    private final FlightUploadBatchRepository batchRepository;
    private final OperationalFlightRepository flightRepository;
    private final FlightConflictRepository conflictRepository;
    private final OperationalFlightMapper flightMapper;
    private final ConflictDetectionService conflictService;
    private final EventPublishService eventPublishService;

    @Async
    @Transactional
    public CompletableFuture<FlightUploadBatchResponseDto> processUpload(
            MultipartFile file, UserContext userContext) {

        FlightUploadBatch batch = createUploadBatch(file, userContext);
        batch = batchRepository.save(batch);

        try {
            batch.setStatus(UploadStatus.PROCESSING);
            batch.setProcessingStartTime(LocalDateTime.now());
            batchRepository.save(batch);

            List<CsvFlightData> csvData = parseCsvFile(file);
            batch.setTotalRows(csvData.size());

            List<OperationalFlightCreateRequestDto> validFlights = new ArrayList<>();
            List<FlightConflict> conflicts = new ArrayList<>();

            for (int i = 0; i < csvData.size(); i++) {
                try {
                    CsvFlightData rowData = csvData.get(i);
                    OperationalFlightCreateRequestDto flightDto = mapCsvToFlightDto(rowData);

                    // Detect conflicts
                    var rowConflicts = conflictService.detectConflicts(flightDto);
                    if (!rowConflicts.isEmpty()) {
                        conflicts.addAll(mapToFlightConflicts(rowConflicts, batch.getId(), i + 1));
                        batch.setConflictRows(batch.getConflictRows() + 1);
                    } else {
                        validFlights.add(flightDto);
                        batch.setSuccessfulRows(batch.getSuccessfulRows() + 1);
                    }

                } catch (Exception e) {
                    batch.setFailedRows(batch.getFailedRows() + 1);
                    log.error("Failed to process CSV row {}: {}", i + 1, e.getMessage());
                }
            }

            // Save conflicts
            if (!conflicts.isEmpty()) {
                conflictRepository.saveAll(conflicts);
            }

            // Process valid flights if no conflicts
            if (conflicts.isEmpty()) {
                processValidFlights(validFlights, batch, userContext);
                batch.setStatus(UploadStatus.COMPLETED);
            } else {
                batch.setStatus(UploadStatus.PROCESSING); // Waiting for conflict resolution
            }

        } catch (Exception e) {
            batch.setStatus(UploadStatus.FAILED);
            log.error("Failed to process CSV upload", e);
        } finally {
            batch.setProcessingEndTime(LocalDateTime.now());
            batchRepository.save(batch);
        }

        return CompletableFuture.completedFuture(mapToBatchResponseDto(batch));
    }

    private FlightUploadBatch createUploadBatch(MultipartFile file, UserContext userContext) {
        return FlightUploadBatch.builder()
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .totalRows(0)
                .uploadedBy(userContext.getUsername())
                .airlineId(userContext.getAirlineId())
                .build();
    }

    private List<CsvFlightData> parseCsvFile(MultipartFile file) throws Exception {
        List<CsvFlightData> result = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext(); // Skip header
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length >= 9) { // Minimum required columns
                    CsvFlightData data = CsvFlightData.builder()
                            .flightNumber(line[0])
                            .airlineCode(line[1])
                            .aircraftType(line[2])
                            .flightDate(line[3])
                            .scheduledDepartureTime(line[4])
                            .scheduledArrivalTime(line[5])
                            .originIcaoCode(line[6])
                            .destinationIcaoCode(line[7])
                            .flightType(line[8])
                            .gate(line.length > 9 ? line[9] : null)
                            .terminal(line.length > 10 ? line[10] : null)
                            .build();
                    result.add(data);
                }
            }
        }

        return result;
    }

    private OperationalFlightCreateRequestDto mapCsvToFlightDto(CsvFlightData csvData) {
        OperationalFlightCreateRequestDto dto = new OperationalFlightCreateRequestDto();

        dto.setFlightNumber(csvData.getFlightNumber());
        dto.setAirlineId(1L); // This should be resolved from airlineCode
        dto.setAircraftId(1L); // This should be resolved from aircraftType
        dto.setFlightDate(LocalDate.parse(csvData.getFlightDate()));
        dto.setScheduledDepartureTime(LocalTime.parse(csvData.getScheduledDepartureTime()));
        dto.setScheduledArrivalTime(LocalTime.parse(csvData.getScheduledArrivalTime()));
        dto.setOriginStationId(1L); // This should be resolved from ICAO code
        dto.setDestinationStationId(2L); // This should be resolved from ICAO code
        dto.setFlightType(FlightType.valueOf(csvData.getFlightType()));
        dto.setGate(csvData.getGate());
        dto.setTerminal(csvData.getTerminal());

        return dto;
    }

    private List<FlightConflict> mapToFlightConflicts(
            List<ConflictDetectionService.Conflict> conflicts, Long batchId, int rowNumber) {

        return conflicts.stream()
                .map(conflict -> FlightConflict.builder()
                        .uploadBatchId(batchId)
                        .rowNumber(rowNumber)
                        .conflictType(conflict.getType())
                        .conflictDescription(conflict.getDescription())
                        .newFlightData("{}")
                        .build())
                .toList();
    }

    private void processValidFlights(List<OperationalFlightCreateRequestDto> flights,
                                     FlightUploadBatch batch, UserContext userContext) {

        for (OperationalFlightCreateRequestDto flightDto : flights) {
            try {
                OperationalFlight flight = flightMapper.toEntity(flightDto);
                flight.setCreatedBy(userContext.getUsername());
                flight.setUpdatedBy(userContext.getUsername());
                flight.setUploadBatchId(batch.getId());

                flightRepository.save(flight);
                eventPublishService.publishFlightEvent("FLIGHT_CREATED", flight, userContext);

            } catch (Exception e) {
                log.error("Failed to save flight from CSV: {}", e.getMessage());
                batch.setFailedRows(batch.getFailedRows() + 1);
                batch.setSuccessfulRows(batch.getSuccessfulRows() - 1);
            }
        }
    }

    private FlightUploadBatchResponseDto mapToBatchResponseDto(FlightUploadBatch batch) {
        FlightUploadBatchResponseDto dto = new FlightUploadBatchResponseDto();
        dto.setId(batch.getId());
        dto.setFileName(batch.getFileName());
        dto.setTotalRows(batch.getTotalRows());
        dto.setSuccessfulRows(batch.getSuccessfulRows());
        dto.setFailedRows(batch.getFailedRows());
        dto.setConflictRows(batch.getConflictRows());
        dto.setStatus(batch.getStatus());
        dto.setUploadedBy(batch.getUploadedBy());
        dto.setCreatedAt(batch.getCreatedAt());
        return dto;
    }

    // Inner class for CSV data
    @lombok.Data
    @lombok.Builder
    public static class CsvFlightData {
        private String flightNumber;
        private String airlineCode;
        private String aircraftType;
        private String flightDate;
        private String scheduledDepartureTime;
        private String scheduledArrivalTime;
        private String originIcaoCode;
        private String destinationIcaoCode;
        private String flightType;
        private String gate;
        private String terminal;
    }
}