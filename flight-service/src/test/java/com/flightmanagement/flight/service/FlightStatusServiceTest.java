package com.flightmanagement.flight.service;

import com.flightmanagement.flight.dto.request.FlightStatusUpdateRequestDto;
import com.flightmanagement.flight.dto.response.DashboardOverviewDto;
import com.flightmanagement.flight.dto.response.LiveFlightStatusDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import com.flightmanagement.flight.enums.FlightStatus;
import com.flightmanagement.flight.exception.FlightNotFoundException;
import com.flightmanagement.flight.exception.UnauthorizedFlightAccessException;
import com.flightmanagement.flight.mapper.OperationalFlightMapperImpl;
import com.flightmanagement.flight.repository.OperationalFlightRepository;
import com.flightmanagement.flight.security.UserContext;
import com.flightmanagement.flight.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightStatusServiceTest {

    @Mock
    private OperationalFlightRepository flightRepository;

    @Mock
    private OperationalFlightMapperImpl flightMapper;

    @Mock
    private EventPublishService eventPublishService;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private FlightStatusService flightStatusService;

    private UserContext adminContext;
    private UserContext airlineUserContext;
    private OperationalFlight flight;
    private FlightStatusUpdateRequestDto statusUpdateRequest;

    @BeforeEach
    void setUp() {
        adminContext = TestDataBuilder.createAdminUserContext();
        airlineUserContext = TestDataBuilder.createAirlineUserContext();
        flight = TestDataBuilder.createValidFlight();
        statusUpdateRequest = TestDataBuilder.createStatusUpdateRequest(FlightStatus.DEPARTED);
    }

    @Test
    void updateFlightStatus_ValidRequest_Success() {
        // Given
        FlightStatus previousStatus = flight.getStatus();
        statusUpdateRequest.setActualDepartureTime(LocalTime.of(10, 35));

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any())).thenReturn(flight);

        OperationalFlightResponseDto responseDto = new OperationalFlightResponseDto();
        responseDto.setId(1L);
        responseDto.setStatus(FlightStatus.DEPARTED);
        when(flightMapper.toResponseDto(any())).thenReturn(responseDto);

        // When
        OperationalFlightResponseDto result = flightStatusService.updateFlightStatus(1L, statusUpdateRequest, adminContext);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(FlightStatus.DEPARTED);

        // Verify flight entity updates
        assertThat(flight.getStatus()).isEqualTo(FlightStatus.DEPARTED);
        assertThat(flight.getActualDepartureTime()).isEqualTo(LocalTime.of(10, 35));
        assertThat(flight.getDepartureDelay()).isEqualTo(5); // 10:35 - 10:30 = 5 minutes
        assertThat(flight.getUpdatedBy()).isEqualTo(adminContext.getUsername());

        // Verify service calls
        verify(flightRepository).save(flight);
        verify(eventPublishService).publishFlightEvent(eq("STATUS_CHANGED"), eq(flight), eq(adminContext));
        verify(webSocketService).notifyFlightStatusUpdate(eq(flight), eq(previousStatus));
    }

    @Test
    void updateFlightStatus_ArrivalUpdate_CalculatesDelay() {
        // Given
        FlightStatusUpdateRequestDto arrivalRequest = TestDataBuilder.createArrivalUpdateRequest();
        arrivalRequest.setActualArrivalTime(LocalTime.of(13, 50)); // 5 minutes late

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any())).thenReturn(flight);
        when(flightMapper.toResponseDto(any())).thenReturn(new OperationalFlightResponseDto());

        // When
        flightStatusService.updateFlightStatus(1L, arrivalRequest, adminContext);

        // Then
        assertThat(flight.getActualArrivalTime()).isEqualTo(LocalTime.of(13, 50));
        assertThat(flight.getArrivalDelay()).isEqualTo(5); // 13:50 - 13:45 = 5 minutes
    }

    @Test
    void updateFlightStatus_DelayedStatus_SetsDelayReason() {
        // Given
        FlightStatusUpdateRequestDto delayRequest = TestDataBuilder.createDelayedUpdateRequest();

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any())).thenReturn(flight);
        when(flightMapper.toResponseDto(any())).thenReturn(new OperationalFlightResponseDto());

        // When
        flightStatusService.updateFlightStatus(1L, delayRequest, adminContext);

        // Then
        assertThat(flight.getStatus()).isEqualTo(FlightStatus.DELAYED);
        assertThat(flight.getDelayReason()).isEqualTo("Weather conditions");
    }

    @Test
    void updateFlightStatus_CancelledStatus_SetsCancellationReason() {
        // Given
        FlightStatusUpdateRequestDto cancelRequest = TestDataBuilder.createCancelledUpdateRequest();

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any())).thenReturn(flight);
        when(flightMapper.toResponseDto(any())).thenReturn(new OperationalFlightResponseDto());

        // When
        flightStatusService.updateFlightStatus(1L, cancelRequest, adminContext);

        // Then
        assertThat(flight.getStatus()).isEqualTo(FlightStatus.CANCELLED);
        assertThat(flight.getCancellationReason()).isEqualTo("Aircraft maintenance");
    }

    @Test
    void updateFlightStatus_FlightNotFound_ThrowsException() {
        // Given
        when(flightRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> flightStatusService.updateFlightStatus(999L, statusUpdateRequest, adminContext))
                .isInstanceOf(FlightNotFoundException.class)
                .hasMessageContaining("Flight not found with id: 999");
    }

    @Test
    void updateFlightStatus_AirlineUserDifferentAirline_ThrowsException() {
        // Given
        flight.setAirlineId(999L); // Different airline
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        // When & Then
        assertThatThrownBy(() -> flightStatusService.updateFlightStatus(1L, statusUpdateRequest, airlineUserContext))
                .isInstanceOf(UnauthorizedFlightAccessException.class)
                .hasMessageContaining("Cannot access flights from different airline");
    }

    @Test
    void getLiveFlightStatus_AdminUser_ReturnsAllFlights() {
        // Given
        LocalDate testDate = LocalDate.now();
        List<OperationalFlight> flights = List.of(
                TestDataBuilder.createValidFlight(),
                TestDataBuilder.createDepartedFlight()
        );
        Page<OperationalFlight> flightPage = new PageImpl<>(flights);

        when(flightRepository.findByFlightDate(eq(testDate), any(Pageable.class)))
                .thenReturn(flightPage);

        // When
        List<LiveFlightStatusDto> result = flightStatusService.getLiveFlightStatus(testDate, adminContext);

        // Then
        assertThat(result).hasSize(2);
        verify(flightRepository).findByFlightDate(eq(testDate), any(Pageable.class));
        verify(flightRepository, never()).findByAirlineIdAndFlightDate(any(), any(), any());
    }

    @Test
    void getLiveFlightStatus_AirlineUser_ReturnsOnlyOwnFlights() {
        // Given
        LocalDate testDate = LocalDate.now();
        List<OperationalFlight> flights = List.of(TestDataBuilder.createValidFlight());
        Page<OperationalFlight> flightPage = new PageImpl<>(flights);

        when(flightRepository.findByAirlineIdAndFlightDate(eq(1L), eq(testDate), any(Pageable.class)))
                .thenReturn(flightPage);

        // When
        List<LiveFlightStatusDto> result = flightStatusService.getLiveFlightStatus(testDate, airlineUserContext);

        // Then
        assertThat(result).hasSize(1);
        verify(flightRepository).findByAirlineIdAndFlightDate(eq(1L), eq(testDate), any(Pageable.class));
    }

    @Test
    void getLiveFlightStatus_MapsFlightCorrectly() {
        // Given
        LocalDate testDate = LocalDate.now();
        OperationalFlight departedFlight = TestDataBuilder.createDepartedFlight();
        departedFlight.setId(1L);
        departedFlight.setFlightNumber("TK123");
        departedFlight.setAirlineCode("TK");
        departedFlight.setAircraftType("A320");
        departedFlight.setOriginIcaoCode("LTBA");
        departedFlight.setDestinationIcaoCode("EDDF");
        departedFlight.setGate("A12");
        departedFlight.setTerminal("1");

        Page<OperationalFlight> flightPage = new PageImpl<>(List.of(departedFlight));
        when(flightRepository.findByFlightDate(eq(testDate), any(Pageable.class)))
                .thenReturn(flightPage);

        // When
        List<LiveFlightStatusDto> result = flightStatusService.getLiveFlightStatus(testDate, adminContext);

        // Then
        assertThat(result).hasSize(1);
        LiveFlightStatusDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFlightNumber()).isEqualTo("TK123");
        assertThat(dto.getAirlineCode()).isEqualTo("TK");
        assertThat(dto.getAircraftType()).isEqualTo("A320");
        assertThat(dto.getOriginIcaoCode()).isEqualTo("LTBA");
        assertThat(dto.getDestinationIcaoCode()).isEqualTo("EDDF");
        assertThat(dto.getGate()).isEqualTo("A12");
        assertThat(dto.getTerminal()).isEqualTo("1");
        assertThat(dto.getStatus()).isEqualTo(FlightStatus.DEPARTED);
        assertThat(dto.getDepartureDelay()).isEqualTo(5);
    }

    @Test
    void getDashboardOverview_AdminUser_CalculatesMetricsCorrectly() {
        // Given
        LocalDate testDate = LocalDate.now();
        List<OperationalFlight> flights = createTestFlightsForDashboard();
        Page<OperationalFlight> flightPage = new PageImpl<>(flights);

        when(flightRepository.findByFlightDate(eq(testDate), any(Pageable.class)))
                .thenReturn(flightPage);

        // When
        DashboardOverviewDto result = flightStatusService.getDashboardOverview(testDate, adminContext);

        // Then
        assertThat(result.getDate()).isEqualTo(testDate);
        assertThat(result.getTotalFlights()).isEqualTo(5);
        assertThat(result.getScheduledFlights()).isEqualTo(2);
        assertThat(result.getDepartedFlights()).isEqualTo(1);
        assertThat(result.getArrivedFlights()).isEqualTo(1);
        assertThat(result.getCancelledFlights()).isEqualTo(1);
        assertThat(result.getActiveAirlines()).isEqualTo(2); // Two different airlines
    }

    @Test
    void getDashboardOverview_AirlineUser_ReturnsOnlyOwnAirlineData() {
        // Given
        LocalDate testDate = LocalDate.now();
        List<OperationalFlight> flights = List.of(TestDataBuilder.createValidFlight());
        Page<OperationalFlight> flightPage = new PageImpl<>(flights);

        when(flightRepository.findByAirlineIdAndFlightDate(eq(1L), eq(testDate), any(Pageable.class)))
                .thenReturn(flightPage);

        // When
        DashboardOverviewDto result = flightStatusService.getDashboardOverview(testDate, airlineUserContext);

        // Then
        assertThat(result.getTotalFlights()).isEqualTo(1);
        assertThat(result.getActiveAirlines()).isEqualTo(1);
        verify(flightRepository).findByAirlineIdAndFlightDate(eq(1L), eq(testDate), any(Pageable.class));
    }

    private List<OperationalFlight> createTestFlightsForDashboard() {
        return List.of(
                TestDataBuilder.createFlightWithStatus(FlightStatus.SCHEDULED),
                TestDataBuilder.createFlightWithStatus(FlightStatus.SCHEDULED),
                TestDataBuilder.createFlightWithStatus(FlightStatus.DEPARTED),
                TestDataBuilder.createFlightWithStatus(FlightStatus.ARRIVED),
                createCancelledFlight()
        );
    }

    private OperationalFlight createCancelledFlight() {
        OperationalFlight flight = TestDataBuilder.createValidFlight();
        flight.setId(5L);
        flight.setAirlineId(2L); // Different airline for testing
        flight.setStatus(FlightStatus.CANCELLED);
        return flight;
    }
}