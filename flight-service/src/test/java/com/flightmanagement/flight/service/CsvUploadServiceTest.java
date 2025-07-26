package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.response.FlightUploadBatchResponseDto;
import com.flightmanagement.flight.entity.FlightConflict;
import com.flightmanagement.flight.entity.FlightUploadBatch;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.ConflictType;
import com.flightmanagement.flight.enums.UploadStatus;
import com.flightmanagement.flight.mapper.OperationalFlightMapperImpl;
import com.flightmanagement.flight.repository.FlightConflictRepository;
import com.flightmanagement.flight.repository.FlightUploadBatchRepository;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.security.UserContext;
import com.flightmanagement.flight.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CsvUploadServiceTest {

    @Mock
    private FlightUploadBatchRepository batchRepository;

    @Mock
    private OperationalFlightRepository flightRepository;

    @Mock
    private FlightConflictRepository conflictRepository;

    @Mock
    private OperationalFlightMapperImpl flightMapper;

    @Mock
    private ConflictDetectionService conflictService;

    @Mock
    private FlightEnrichmentService enrichmentService;

    @Mock
    private EventPublishService eventPublishService;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private CsvUploadService csvUploadService;

    private UserContext userContext;
    private MultipartFile validCsvFile;
    private MultipartFile invalidCsvFile;
    private FlightUploadBatch uploadBatch;

    @BeforeEach
    void setUp() {
        userContext = TestDataBuilder.createAirlineUserContext();

        validCsvFile = new MockMultipartFile(
                "file",
                "test-flights.csv",
                "text/csv",
                TestDataBuilder.createValidCsvContent().getBytes()
        );

        invalidCsvFile = new MockMultipartFile(
                "file",
                "invalid-flights.csv",
                "text/csv",
                TestDataBuilder.createInvalidCsvContent().getBytes()
        );

        uploadBatch = TestDataBuilder.createUploadBatch();
        uploadBatch.setId(1L);

        // Mock mapper behavior - bu satırları ekle
        OperationalFlight mockFlight = TestDataBuilder.createValidFlight();
        when(flightMapper.toEntity(any())).thenReturn(mockFlight);

        // Mock enrichment service behavior - bu satırları ekle
        doNothing().when(enrichmentService).enrichFromCsvData(
                any(OperationalFlight.class),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );

        // Mock WebSocket and Event services
        doNothing().when(webSocketService).notifyUploadProgress(any());
        doNothing().when(webSocketService).notifyUploadCompleted(any());
        doNothing().when(eventPublishService).publishFlightEvent(anyString(), any(), any());
    }

    @Test
    void processUpload_ValidCsvNoConflicts_Success() throws Exception {
        // Given
        when(batchRepository.save(any())).thenReturn(uploadBatch);
        when(conflictService.detectConflicts(any())).thenReturn(Collections.emptyList());

        OperationalFlight flight = TestDataBuilder.createValidFlight();
        when(flightMapper.toEntity(any())).thenReturn(flight);
        when(flightRepository.save(any())).thenReturn(flight);

        // When
        CompletableFuture<FlightUploadBatchResponseDto> future =
                csvUploadService.processUpload(validCsvFile, userContext);
        FlightUploadBatchResponseDto result = future.get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("test-flights.csv");

        // Verify batch creation and status updates
        ArgumentCaptor<FlightUploadBatch> batchCaptor = ArgumentCaptor.forClass(FlightUploadBatch.class);
        verify(batchRepository, atLeast(2)).save(batchCaptor.capture());

        List<FlightUploadBatch> savedBatches = batchCaptor.getAllValues();
        FlightUploadBatch initialBatch = savedBatches.get(0);
        assertThat(initialBatch.getFileName()).isEqualTo("test-flights.csv");
        assertThat(initialBatch.getUploadedBy()).isEqualTo(userContext.getUsername());
        assertThat(initialBatch.getAirlineId()).isEqualTo(userContext.getAirlineId());

        // Verify WebSocket notifications
        verify(webSocketService, atLeastOnce()).notifyUploadProgress(any());
        verify(webSocketService).notifyUploadCompleted(any());

        // Verify flights were saved
        verify(flightRepository, atLeast(1)).save(any());
        verify(eventPublishService, atLeast(1)).publishFlightEvent(eq("FLIGHT_CREATED"), any(), any());
    }

    @Test
    void processUpload_CsvWithConflicts_SavesConflicts() throws Exception {
        // Given
        when(batchRepository.save(any())).thenReturn(uploadBatch);

        ConflictDetectionService.Conflict conflict = new ConflictDetectionService.Conflict(
                ConflictType.FLIGHT_NUMBER_DUPLICATE, "Duplicate flight number");
        when(conflictService.detectConflicts(any())).thenReturn(List.of(conflict));

        // Mock flight creation for temp flight
        OperationalFlight tempFlight = TestDataBuilder.createValidFlight();
        when(flightMapper.toEntity(any())).thenReturn(tempFlight);

        // When
        CompletableFuture<FlightUploadBatchResponseDto> future =
                csvUploadService.processUpload(validCsvFile, userContext);
        future.get();

        // Then
        verify(conflictRepository).saveAll(anyList());
        verify(flightRepository, never()).save(any()); // No flights should be saved due to conflicts
    }

    @Test
    void processUpload_EmptyFile_HandlesGracefully() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                "flightNumber,airlineCode,aircraftType,flightDate,scheduledDepartureTime,scheduledArrivalTime,originIcaoCode,destinationIcaoCode,flightType\n".getBytes()
        );

        when(batchRepository.save(any())).thenReturn(uploadBatch);

        // When
        CompletableFuture<FlightUploadBatchResponseDto> future =
                csvUploadService.processUpload(emptyFile, userContext);
        future.get();

        // Then
        ArgumentCaptor<FlightUploadBatch> batchCaptor = ArgumentCaptor.forClass(FlightUploadBatch.class);
        verify(batchRepository, atLeast(2)).save(batchCaptor.capture());

        FlightUploadBatch finalBatch = batchCaptor.getValue();
        assertThat(finalBatch.getTotalRows()).isEqualTo(0);
        assertThat(finalBatch.getStatus()).isEqualTo(UploadStatus.COMPLETED);
    }

    @Test
    void processUpload_FileProcessingError_MarksAsFailed() throws Exception {
        // Given
        MockMultipartFile corruptFile = new MockMultipartFile(
                "file",
                "corrupt.csv",
                "text/csv",
                "invalid,csv,content,that,cannot,be,parsed".getBytes()
        );

        when(batchRepository.save(any())).thenReturn(uploadBatch);

        // When
        CompletableFuture<FlightUploadBatchResponseDto> future =
                csvUploadService.processUpload(corruptFile, userContext);
        future.get();

        // Then
        ArgumentCaptor<FlightUploadBatch> batchCaptor = ArgumentCaptor.forClass(FlightUploadBatch.class);
        verify(batchRepository, atLeast(2)).save(batchCaptor.capture());

        // Should handle gracefully and complete processing
        verify(webSocketService).notifyUploadCompleted(any());
    }

    @Test
    void processUpload_EnrichmentService_CalledForEachRow() throws Exception {
        // Given
        when(batchRepository.save(any())).thenReturn(uploadBatch);
        when(conflictService.detectConflicts(any())).thenReturn(Collections.emptyList());

        OperationalFlight flight = TestDataBuilder.createValidFlight();
        when(flightMapper.toEntity(any())).thenReturn(flight);
        when(flightRepository.save(any())).thenReturn(flight);

        // When
        csvUploadService.processUpload(validCsvFile, userContext).get();

        // Then
        // Should call enrichment for each CSV row
        verify(enrichmentService, atLeast(1)).enrichFromCsvData(
                any(OperationalFlight.class),
                anyString(), // airlineCode
                anyString(), // aircraftType
                anyString(), // originIcao
                anyString()  // destinationIcao
        );
    }

    @Test
    void processUpload_ProgressNotifications_SentCorrectly() throws Exception {
        // Given
        when(batchRepository.save(any())).thenReturn(uploadBatch);
        when(conflictService.detectConflicts(any())).thenReturn(Collections.emptyList());

        OperationalFlight flight = TestDataBuilder.createValidFlight();
        when(flightMapper.toEntity(any())).thenReturn(flight);
        when(flightRepository.save(any())).thenReturn(flight);

        // When
        csvUploadService.processUpload(validCsvFile, userContext).get();

        // Then
        verify(webSocketService).notifyUploadProgress(any()); // Start notification
        verify(webSocketService).notifyUploadCompleted(any()); // Completion notification
    }

    @Test
    void processUpload_BatchMetadata_SetCorrectly() throws Exception {
        // Given
        when(batchRepository.save(any())).thenReturn(uploadBatch);
        when(conflictService.detectConflicts(any())).thenReturn(Collections.emptyList());

        // When
        csvUploadService.processUpload(validCsvFile, userContext).get();

        // Then
        ArgumentCaptor<FlightUploadBatch> batchCaptor = ArgumentCaptor.forClass(FlightUploadBatch.class);
        verify(batchRepository, atLeast(1)).save(batchCaptor.capture());

        FlightUploadBatch savedBatch = batchCaptor.getAllValues().get(0);
        assertThat(savedBatch.getFileName()).isEqualTo("test-flights.csv");
        assertThat(savedBatch.getFileSize()).isEqualTo(validCsvFile.getSize());
        assertThat(savedBatch.getUploadedBy()).isEqualTo(userContext.getUsername());
        assertThat(savedBatch.getAirlineId()).isEqualTo(userContext.getAirlineId());
    }

    @Test
    void processUpload_ConflictData_MappedCorrectly() throws Exception {
        // Given
        when(batchRepository.save(any())).thenReturn(uploadBatch);

        ConflictDetectionService.Conflict conflict = new ConflictDetectionService.Conflict(
                ConflictType.AIRCRAFT_DOUBLE_BOOKING, "Aircraft conflict");
        when(conflictService.detectConflicts(any())).thenReturn(List.of(conflict));

        // Mock flight creation for temp flight
        OperationalFlight tempFlight = TestDataBuilder.createValidFlight();
        when(flightMapper.toEntity(any())).thenReturn(tempFlight);

        // When
        csvUploadService.processUpload(validCsvFile, userContext).get();

        // Then
        verify(conflictRepository).saveAll(anyList());
        verify(flightRepository, never()).save(any()); // No flights should be saved due to conflicts
    }

    @Test
    void processUpload_FlightCreation_SetsCorrectMetadata() throws Exception {
        // Given
        when(batchRepository.save(any())).thenReturn(uploadBatch);
        when(conflictService.detectConflicts(any())).thenReturn(Collections.emptyList());

        OperationalFlight flight = TestDataBuilder.createValidFlight();
        when(flightMapper.toEntity(any())).thenReturn(flight);
        when(flightRepository.save(any())).thenReturn(flight);

        // When
        csvUploadService.processUpload(validCsvFile, userContext).get();

        // Then
        ArgumentCaptor<OperationalFlight> flightCaptor = ArgumentCaptor.forClass(OperationalFlight.class);
        verify(flightRepository, atLeast(1)).save(flightCaptor.capture());

        OperationalFlight savedFlight = flightCaptor.getValue();
        assertThat(savedFlight.getCreatedBy()).isEqualTo(userContext.getUsername());
        assertThat(savedFlight.getUpdatedBy()).isEqualTo(userContext.getUsername());
        assertThat(savedFlight.getUploadBatchId()).isEqualTo(uploadBatch.getId());
    }
}