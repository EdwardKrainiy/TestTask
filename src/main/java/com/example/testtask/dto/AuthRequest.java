package com.example.testtask.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    
    @NotBlank(message = "Login cannot be blank")
    private String login; // email or phone
    
    @NotBlank(message = "Password cannot be blank")
    private String password;
} 