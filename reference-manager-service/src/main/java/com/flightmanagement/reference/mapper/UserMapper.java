package com.flightmanagement.reference.mapper;

import com.flightmanagement.reference.dto.response.UserResponseDto;
import com.flightmanagement.reference.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "airlineName", ignore = true)
    UserResponseDto toResponseDto(User user);
}