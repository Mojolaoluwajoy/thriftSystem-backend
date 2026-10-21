package com.thrift.thriftsystem.controller;

import com.thrift.thriftsystem.dto.request.LoginRequest;
import com.thrift.thriftsystem.dto.request.RegisterRequest;
import com.thrift.thriftsystem.dto.response.ApiResponse;
import com.thrift.thriftsystem.dto.response.AuthResponse;
import com.thrift.thriftsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ApiResponse> register(
            @RequestBody @Valid RegisterRequest registerRequest) {

        AuthResponse authResponse = authService.register(registerRequest);
        return new ResponseEntity<>(
                ApiResponse.success("Login successful", authResponse), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return new ResponseEntity<>(
                ApiResponse.success("Login successful", authResponse), HttpStatus.OK);
    }
}
