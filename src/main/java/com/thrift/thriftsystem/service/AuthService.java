package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.dto.request.LoginRequest;
import com.thrift.thriftsystem.dto.request.RegisterRequest;
import com.thrift.thriftsystem.dto.response.AuthResponse;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.exception.DuplicateResourceException;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.security.JwtUtil;
import com.thrift.thriftsystem.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final UserMapper userMapper;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("Phone number already registered");
        }
        User user = userMapper.toModel(request, passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
        emailService.sendWelcomeEmail(user);

        String token = jwtUtil.generateToken(user.getEmail());
        log.info("User registered successfully : {}", user.getEmail());

        return userMapper.toAuthResponse(user, token);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new BadRequestException("Account is disabled");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        log.info("User Logged in successfully : {}", user.getEmail());
        return userMapper.toAuthResponse(user, token);
    }
}
