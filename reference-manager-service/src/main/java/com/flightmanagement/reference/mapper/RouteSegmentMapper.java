package com.flightmanagement.reference.mapper;

import com.flightmanagement.reference.dto.request.RouteSegmentCreateRequestDto;
import com.flightmanagement.reference.dto.response.RouteSegmentResponseDto;
import com.flightmanagement.reference.entity.RouteSegment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {StationMapper.class})
public interface RouteSegmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flight", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RouteSegment toEntity(RouteSegmentCreateRequestDto dto);

    @Mapping(target = "originStation", source = "originStationId")
    @Mapping(target = "destinationStation", source = "destinationStationId")
    RouteSegmentResponseDto toResponseDto(RouteSegment segment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flight", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(RouteSegmentCreateRequestDto dto, @MappingTarget RouteSegment segment);
}