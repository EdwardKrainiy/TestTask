package com.example.testtask.controller;

import com.example.testtask.dto.*;
import com.example.testtask.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user management operations")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user with account, emails and phones")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("User creation request received: name={}", request.getName());

        UserResponse response = userService.createUser(request);

        log.info("User creation request completed successfully: userID={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get user by ID", description = "Retrieves user information by user ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        log.debug("Get user request received: userID={}", userId);

        Optional<UserResponse> user = userService.getUserById(userId);
        if (user.isPresent()) {
            log.debug("Get user request completed successfully: userID={}", userId);
            return ResponseEntity.ok(user.get());
        } else {
            log.debug("Get user request: user not found - userID={}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{userId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update user", description = "Updates user information")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {

        Long currentUserId = Long.valueOf(authentication.getName());
        log.info("User update request received: userID={}, requestorID={}", userId, currentUserId);

        if (!currentUserId.equals(userId)) {
            log.warn("User update request rejected: insufficient permissions - userID={}, requestorID={}",
                    userId, currentUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<UserResponse> user = userService.updateUser(userId, request);
        if (user.isPresent()) {
            log.info("User update request completed successfully: userID={}", userId);
            return ResponseEntity.ok(user.get());
        } else {
            log.warn("User update request: user not found - userID={}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Search users", description = "Search users with filtering and pagination")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @Parameter(description = "Filter by date of birth (users born after this date)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,

            @Parameter(description = "Filter by phone number (partial match)")
            @RequestParam(required = false) String phone,

            @Parameter(description = "Filter by name (partial match, case insensitive)")
            @RequestParam(required = false) String name,

            @Parameter(description = "Filter by email (partial match)")
            @RequestParam(required = false) String email,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {

        log.info("User search request received: dateOfBirth={}, phone={}, name={}, email={}, page={}, size={}",
                dateOfBirth, phone, name, email, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponse> users = userService.searchUsers(dateOfBirth, phone, name, email, pageable);

        log.info("User search request completed successfully: found {} users", users.getNumberOfElements());
        return ResponseEntity.ok(users);
    }
}
