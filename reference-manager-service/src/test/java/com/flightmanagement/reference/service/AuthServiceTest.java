package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.request.LoginRequestDto;
import com.flightmanagement.reference.dto.request.UserRegisterRequestDto;
import com.flightmanagement.reference.dto.response.JwtResponseDto;
import com.flightmanagement.reference.entity.User;
import com.flightmanagement.reference.enums.UserRole;
import com.flightmanagement.reference.exception.DuplicateReferenceException;
import com.flightmanagement.reference.mapper.UserMapper;
import com.flightmanagement.reference.repository.UserRepository;
import com.flightmanagement.reference.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private UserRegisterRequestDto registerRequest;
    private LoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new UserRegisterRequestDto();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("Password123");
        registerRequest.setEmail("test@example.com");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
        registerRequest.setRole(UserRole.AIRLINE_USER);
        registerRequest.setAirlineId(1L);

        loginRequest = new LoginRequestDto();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Password123");
    }

    @Test
    void registerUser_Success() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When
        authService.registerUser(registerRequest);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_DuplicateUsername_ThrowsException() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(registerRequest))
                .isInstanceOf(DuplicateReferenceException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void login_Success() {
        // Given
        Authentication authentication = mock(Authentication.class);
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(userMapper.toResponseDto(any())).thenReturn(null);

        // When
        JwtResponseDto response = authService.login(loginRequest);

        // Then
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
    }
}