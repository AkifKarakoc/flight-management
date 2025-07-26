package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.request.LoginRequestDto;
import com.flightmanagement.reference.dto.request.UserRegisterRequestDto;
import com.flightmanagement.reference.dto.response.JwtResponseDto;
import com.flightmanagement.reference.dto.response.UserResponseDto;
import com.flightmanagement.reference.entity.User;
import com.flightmanagement.reference.exception.DuplicateReferenceException;
import com.flightmanagement.reference.mapper.UserMapper;
import com.flightmanagement.reference.repository.UserRepository;
import com.flightmanagement.reference.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    public void registerUser(UserRegisterRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateReferenceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateReferenceException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .airlineId(request.getAirlineId())
                .role(request.getRole())
                .isActive(false)
                .isApproved(false)
                .build();

        userRepository.save(user);
    }

    public JwtResponseDto login(LoginRequestDto request) {
        System.out.println("=== Login attempt for: " + request.getUsername());
        System.out.println("=== Password provided: " + request.getPassword());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            System.out.println("=== Authentication successful!");

            String token = tokenProvider.generateToken(authentication);
            User user = userRepository.findByUsername(request.getUsername()).orElseThrow();

            return JwtResponseDto.builder()
                    .token(token)
                    .expiresIn(86400000L)
                    .userInfo(userMapper.toResponseDto(user))
                    .build();

        } catch (Exception e) {
            System.out.println("=== Authentication failed: " + e.getMessage());
            throw e;
        }
    }
}