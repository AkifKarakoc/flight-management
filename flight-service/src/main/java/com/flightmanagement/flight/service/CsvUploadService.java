package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.dto.response.FlightUploadBatchResponseDto;
import com.flightmanagement.flight.entity.FlightConflict;
import com.flightmanagement.flight.entity.FlightUploadBatch;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.FlightType;
import com.flightmanagement.flight.enums.UploadStatus;
import com.flightmanagement.flight.mapper.OperationalFlightMapperImpl;
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
    private final OperationalFlightMapperImpl flightMapper;
    private final ConflictDetectionService conflictService;
    private final FlightEnrichmentService enrichmentService;
    private final EventPublishService eventPublishService;
    private final WebSocketService webSocketService;

    @Async("csvProcessingExecutor")
    @Transactional
    public CompletableFuture<FlightUploadBatchResponseDto> processUpload(
            MultipartFile file, UserContext userContext) {

        FlightUploadBatch batch = createUploadBatch(file, userContext);
        batch = batchRepository.save(batch);

        try {
            batch.setStatus(UploadStatus.PROCESSING);
            batch.setProcessingStartTime(LocalDateTime.now());
            batchRepository.save(batch);

            // Notify upload start
            webSocketService.notifyUploadProgress(batch);

            List<CsvFlightRow> csvData = parseCsvFile(file);
            batch.setTotalRows(csvData.size());

            List<FlightCreationData> validFlights = new ArrayList<>();
            List<FlightConflict> conflicts = new ArrayList<>();

            for (int i = 0; i < csvData.size(); i++) {
                try {
                    CsvFlightRow rowData = csvData.get(i);

                    // Create enriched flight data
                    FlightCreationData flightData = enrichCsvRow(rowData, userContext);

                    // Detect conflicts using the enriched data
                    var rowConflicts = conflictService.detectConflicts(flightData.getFlightDto());
                    if (!rowConflicts.isEmpty()) {
                        conflicts.addAll(mapToFlightConflicts(rowConflicts, batch.getId(), i + 1, rowData));
                        batch.setConflictRows(batch.getConflictRows() + 1);
                    } else {
                        validFlights.add(flightData);
                        batch.setSuccessfulRows(batch.getSuccessfulRows() + 1);
                    }

                    // Update progress periodically
                    if ((i + 1) % 10 == 0) {
                        batchRepository.save(batch);
                        webSocketService.notifyUploadProgress(batch);
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
            webSocketService.notifyUploadCompleted(batch);
        }

        return CompletableFuture.completedFuture(mapToBatchResponseDto(batch));
    }

    private FlightCreationData enrichCsvRow(CsvFlightRow csvRow, UserContext userContext) {
        // Create basic DTO
        OperationalFlightCreateRequestDto dto = mapCsvToFlightDto(csvRow);

        // Create temporary flight entity for enrichment
        OperationalFlight tempFlight = flightMapper.toEntity(dto);

        // Enrich with reference data
        enrichmentService.enrichFromCsvData(tempFlight,
                csvRow.getAirlineCode(),
                csvRow.getAircraftType(),
                csvRow.getOriginIcaoCode(),
                csvRow.getDestinationIcaoCode());

        // Update DTO with enriched IDs
        if (tempFlight.getAirlineId() != null) {
            dto.setAirlineId(tempFlight.getAirlineId());
        }
        if (tempFlight.getAircraftId() != null) {
            dto.setAircraftId(tempFlight.getAircraftId());
        }
        if (tempFlight.getOriginStationId() != null) {
            dto.setOriginStationId(tempFlight.getOriginStationId());
        }
        if (tempFlight.getDestinationStationId() != null) {
            dto.setDestinationStationId(tempFlight.getDestinationStationId());
        }

        return new FlightCreationData(dto, csvRow, tempFlight);
    }

    private List<CsvFlightRow> parseCsvFile(MultipartFile file) throws Exception {
        List<CsvFlightRow> result = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext(); // Skip header
            String[] line;
            int rowNumber = 1;

            while ((line = reader.readNext()) != null) {
                rowNumber++;
                if (line.length >= 9) { // Minimum required columns
                    CsvFlightRow data = CsvFlightRow.builder()
                            .rowNumber(rowNumber)
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

    private OperationalFlightCreateRequestDto mapCsvToFlightDto(CsvFlightRow csvData) {
        OperationalFlightCreateRequestDto dto = new OperationalFlightCreateRequestDto();

        dto.setFlightNumber(csvData.getFlightNumber());
        dto.setFlightDate(LocalDate.parse(csvData.getFlightDate()));
        dto.setScheduledDepartureTime(LocalTime.parse(csvData.getScheduledDepartureTime()));
        dto.setScheduledArrivalTime(LocalTime.parse(csvData.getScheduledArrivalTime()));
        dto.setFlightType(FlightType.valueOf(csvData.getFlightType()));
        dto.setGate(csvData.getGate());
        dto.setTerminal(csvData.getTerminal());

        // Set temporary IDs - these will be resolved by enrichment service
        dto.setAirlineId(1L);
        dto.setAircraftId(1L);
        dto.setOriginStationId(1L);
        dto.setDestinationStationId(1L);

        return dto;
    }

    private List<FlightConflict> mapToFlightConflicts(
            List<ConflictDetectionService.Conflict> conflicts, Long batchId, int rowNumber, CsvFlightRow csvData) {

        return conflicts.stream()
                .map(conflict -> FlightConflict.builder()
                        .uploadBatchId(batchId)
                        .rowNumber(rowNumber)
                        .conflictType(conflict.getType())
                        .conflictDescription(conflict.getDescription())
                        .newFlightData(formatCsvRowAsJson(csvData))
                        .build())
                .toList();
    }

    private void processValidFlights(List<FlightCreationData> flightDataList,
                                     FlightUploadBatch batch, UserContext userContext) {

        for (FlightCreationData flightData : flightDataList) {
            try {
                OperationalFlight flight = flightData.getEnrichedFlight();
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

    private String formatCsvRowAsJson(CsvFlightRow csvData) {
        return String.format("""
            {
                "flightNumber": "%s",
                "airlineCode": "%s",
                "aircraftType": "%s",
                "flightDate": "%s",
                "scheduledDepartureTime": "%s",
                "scheduledArrivalTime": "%s",
                "originIcaoCode": "%s",
                "destinationIcaoCode": "%s",
                "flightType": "%s",
                "gate": "%s",
                "terminal": "%s"
            }
            """,
                csvData.getFlightNumber(), csvData.getAirlineCode(), csvData.getAircraftType(),
                csvData.getFlightDate(), csvData.getScheduledDepartureTime(), csvData.getScheduledArrivalTime(),
                csvData.getOriginIcaoCode(), csvData.getDestinationIcaoCode(), csvData.getFlightType(),
                csvData.getGate(), csvData.getTerminal());
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

    // Inner classes
    @lombok.Data
    @lombok.Builder
    public static class CsvFlightRow {
        private int rowNumber;
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

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class FlightCreationData {
        private OperationalFlightCreateRequestDto flightDto;
        private CsvFlightRow csvRow;
        private OperationalFlight enrichedFlight;
    }
}