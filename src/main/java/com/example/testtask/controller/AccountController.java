package com.example.testtask.controller;

import com.example.testtask.dto.TransferRequest;
import com.example.testtask.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management", description = "APIs for account and money transfer operations")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {
    
    private final AccountService accountService;
    
    @PostMapping("/transfer")
    @Operation(summary = "Transfer money", description = "Transfer money from current user to another user")
    public ResponseEntity<Void> transferMoney(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {
        try {
            Long fromUserId = Long.valueOf(authentication.getName());
            accountService.transferMoney(fromUserId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Error transferring money: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error during money transfer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 