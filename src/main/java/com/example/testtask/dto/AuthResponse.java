package com.example.testtask.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    private String type = "Bearer";
    
    public AuthResponse(String token) {
        this.token = token;
    }
} 