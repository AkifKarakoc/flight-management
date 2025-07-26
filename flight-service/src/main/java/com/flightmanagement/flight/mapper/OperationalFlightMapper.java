package com.flightmanagement.flight.mapper;

import com.flightmanagement.flight.dto.request.OperationalFlightCreateRequestDto;
import com.flightmanagement.flight.dto.response.OperationalFlightResponseDto;
import com.flightmanagement.flight.entity.OperationalFlight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OperationalFlightMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "airlineCode", ignore = true)
    @Mapping(target = "airlineName", ignore = true)
    @Mapping(target = "aircraftType", ignore = true)
    @Mapping(target = "originIcaoCode", ignore = true)
    @Mapping(target = "destinationIcaoCode", ignore = true)
    @Mapping(target = "actualDepartureTime", ignore = true)
    @Mapping(target = "actualArrivalTime", ignore = true)
    @Mapping(target = "departureDelay", ignore = true)
    @Mapping(target = "arrivalDelay", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "delayReason", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uploadBatchId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    OperationalFlight toEntity(OperationalFlightCreateRequestDto dto);

    OperationalFlightResponseDto toResponseDto(OperationalFlight flight);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDto(OperationalFlightCreateRequestDto dto, @MappingTarget OperationalFlight flight);
}