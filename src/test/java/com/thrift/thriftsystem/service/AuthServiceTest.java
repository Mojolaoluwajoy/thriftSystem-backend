package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.request.LoginRequest;
import com.thrift.thriftsystem.dto.request.RegisterRequest;
import com.thrift.thriftsystem.dto.response.AuthResponse;
import com.thrift.thriftsystem.exception.DuplicateResourceException;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.security.JwtUtil;
import com.thrift.thriftsystem.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPhoneNumber("08012345678");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        user = User.builder()
                .id("user123")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("08012345678")
                .password("encodedPassword")
                .enabled(true)
                .build();
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userMapper.toModel(any(), any())).thenReturn(user);
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtil.generateToken(any())).thenReturn("token123");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("token123", response.getToken());
        assertEquals("Bearer", response.getType());
        verify(userRepository, times(1)).save(any());
        verify(emailService, times(1)).sendWelcomeEmail(any());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail(any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> authService.register(registerRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_DuplicatePhone_ThrowsException() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> authService.register(registerRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any())).thenReturn("token123");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("token123", response.getToken());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void login_DisabledAccount_ThrowsException() {
        user.setEnabled(false);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(user));

        assertThrows(Exception.class,
                () -> authService.login(loginRequest));
    }
}