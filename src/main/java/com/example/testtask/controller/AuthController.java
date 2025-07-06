package com.example.testtask.controller;

import com.example.testtask.dto.AuthRequest;
import com.example.testtask.dto.AuthResponse;
import com.example.testtask.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
        
        try {
            AuthResponse response = userService.authenticate(request);
            log.info("Authentication request completed successfully: login={}", request.getLogin());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Authentication request rejected: login={}, reason={}", 
                    request.getLogin(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Authentication request failed due to technical error: login={}, error={}", 
                    request.getLogin(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 