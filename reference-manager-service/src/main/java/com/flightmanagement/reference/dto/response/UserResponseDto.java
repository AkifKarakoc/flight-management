package com.flightmanagement.reference.dto.response;

import com.flightmanagement.reference.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDto {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Long airlineId;
    private String airlineName;
    private UserRole role;
    private Boolean isActive;
    private Boolean isApproved;
    private LocalDateTime createdAt;
}