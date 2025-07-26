package com.flightmanagement.flight.security;

import com.flightmanagement.flight.config.FlightServiceProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Component
@Slf4j
public class JwtValidationService {

    private final SecretKey key;

    public JwtValidationService(FlightServiceProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes());
    }

    public UserContext validateAndExtractUser(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return UserContext.builder()
                    .userId(claims.get("userId", Long.class))
                    .username(claims.getSubject())
                    .airlineId(claims.get("airlineId", Long.class))
                    .roles(claims.get("roles", List.class))
                    .build();

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token");
        }
    }
}