package com.flightmanagement.flight.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightmanagement.flight.config.FlightTestConfiguration;
import com.flightmanagement.flight.security.UserContext;
import com.flightmanagement.flight.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(FlightTestConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "logging.level.org.springframework.security=INFO",
        "logging.level.com.flightmanagement.flight=DEBUG"
})
@Transactional
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    protected UserContext adminContext;
    protected UserContext airlineUserContext;
    protected UsernamePasswordAuthenticationToken adminAuth;
    protected UsernamePasswordAuthenticationToken airlineUserAuth;
    protected String baseUrl;

    @BeforeEach
    void setUpBaseTest() {
        baseUrl = "http://localhost:" + port + "/flight-service";

        // Create test user contexts
        adminContext = TestDataBuilder.createAdminUserContext();
        airlineUserContext = TestDataBuilder.createAirlineUserContext();

        // Create Spring Security authentication objects
        adminAuth = new UsernamePasswordAuthenticationToken(
                adminContext,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        airlineUserAuth = new UsernamePasswordAuthenticationToken(
                airlineUserContext,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_AIRLINE_USER"))
        );
    }

    protected String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}