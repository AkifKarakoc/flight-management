package com.flightmanagement.reference.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class JwtResponseDto {

    private String token;

    @Builder.Default
    private String type = "Bearer";

    private Long expiresIn;

    private UserResponseDto userInfo;
}