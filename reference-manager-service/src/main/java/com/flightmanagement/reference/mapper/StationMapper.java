package com.flightmanagement.reference.mapper;

import com.flightmanagement.reference.dto.request.StationCreateRequestDto;
import com.flightmanagement.reference.dto.response.StationResponseDto;
import com.flightmanagement.reference.entity.Station;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StationMapper {

    StationResponseDto toResponseDto(Station station);

    Station toEntity(StationCreateRequestDto dto);

    void updateEntityFromDto(StationCreateRequestDto dto, @MappingTarget Station station);
}