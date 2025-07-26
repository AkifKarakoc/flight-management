package com.flightmanagement.reference.controller;

import com.flightmanagement.reference.dto.request.LoginRequestDto;
import com.flightmanagement.reference.dto.request.UserRegisterRequestDto;
import com.flightmanagement.reference.dto.response.JwtResponseDto;
import com.flightmanagement.reference.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegisterRequestDto request) {
        authService.registerUser(request);
        return ResponseEntity.ok("User registered successfully. Waiting for admin approval.");
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        JwtResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}