package com.flightmanagement.reference.mapper;

import com.flightmanagement.reference.dto.request.AircraftCreateRequestDto;
import com.flightmanagement.reference.dto.response.AircraftResponseDto;
import com.flightmanagement.reference.entity.Aircraft;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AircraftMapper {

    AircraftResponseDto toResponseDto(Aircraft aircraft);

    Aircraft toEntity(AircraftCreateRequestDto dto);

    void updateEntityFromDto(AircraftCreateRequestDto dto, @MappingTarget Aircraft aircraft);
}