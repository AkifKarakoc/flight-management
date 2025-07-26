package com.flightmanagement.reference.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightmanagement.reference.dto.request.LoginRequestDto;
import com.flightmanagement.reference.dto.request.UserRegisterRequestDto;
import com.flightmanagement.reference.dto.response.JwtResponseDto;
import com.flightmanagement.reference.enums.UserRole;
import com.flightmanagement.reference.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_ValidRequest_ReturnsSuccess() throws Exception {
        // Given
        UserRegisterRequestDto request = new UserRegisterRequestDto();
        request.setUsername("testuser");
        request.setPassword("Password123");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole(UserRole.AIRLINE_USER);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully. Waiting for admin approval."));
    }

    @Test
    void login_ValidCredentials_ReturnsJwtToken() throws Exception {
        // Given
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("testuser");
        request.setPassword("Password123");

        JwtResponseDto response = JwtResponseDto.builder()
                .token("jwt-token")
                .type("Bearer")
                .expiresIn(86400000L)
                .build();

        when(authService.login(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    void register_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        UserRegisterRequestDto request = new UserRegisterRequestDto();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}