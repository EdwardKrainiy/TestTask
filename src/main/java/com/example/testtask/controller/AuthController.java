package com.example.testtask.controller;

import com.example.testtask.dto.AuthRequest;
import com.example.testtask.dto.AuthResponse;
import com.example.testtask.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {
    
    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with email/phone and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Authentication request received: login={}", request.getLogin());

        AuthResponse response = userService.authenticate(request);

        log.info("Authentication request completed successfully: login={}", request.getLogin());
        return ResponseEntity.ok(response);
    }
} 