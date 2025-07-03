package com.example.testtask.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    
    @NotNull(message = "Transfer to user ID is required")
    @Positive(message = "Transfer to user ID must be positive")
    private Long transferTo;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
} 