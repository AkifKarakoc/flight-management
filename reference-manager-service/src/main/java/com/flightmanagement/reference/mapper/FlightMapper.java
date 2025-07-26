package com.flightmanagement.reference.mapper;

import com.flightmanagement.reference.dto.request.FlightCreateRequestDto;
import com.flightmanagement.reference.dto.response.FlightResponseDto;
import com.flightmanagement.reference.entity.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {AirlineMapper.class, AircraftMapper.class, RouteSegmentMapper.class})
public interface FlightMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "segments", ignore = true)
    Flight toEntity(FlightCreateRequestDto dto);

    @Mapping(target = "airline", source = "airlineId")
    @Mapping(target = "aircraft", source = "aircraftId")
    FlightResponseDto toResponseDto(Flight flight);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "segments", ignore = true)
    void updateEntityFromDto(FlightCreateRequestDto dto, @MappingTarget Flight flight);
}