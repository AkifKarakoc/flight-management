package com.flightmanagement.reference.security;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserContext {

    private Long userId;
    private String username;
    private Long airlineId;
    private List<String> roles;

    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    public boolean isAirlineUser() {
        return roles != null && roles.contains("ROLE_AIRLINE_USER");
    }
}