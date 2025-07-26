package com.flightmanagement.reference.mapper;

import com.flightmanagement.reference.dto.request.AirlineCreateRequestDto;
import com.flightmanagement.reference.dto.response.AirlineResponseDto;
import com.flightmanagement.reference.entity.Airline;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AirlineMapper {

    AirlineResponseDto toResponseDto(Airline airline);

    Airline toEntity(AirlineCreateRequestDto dto);

    void updateEntityFromDto(AirlineCreateRequestDto dto, @MappingTarget Airline airline);
}