package com.example.testtask.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRequest {
    
    @Size(max = 500, message = "Name cannot exceed 500 characters")
    private String name;
    
    @Valid
    private List<EmailUpdateRequest> emails;
    
    @Valid
    private List<PhoneUpdateRequest> phones;
    
    @Data
    public static class EmailUpdateRequest {
        @Email(message = "Email should be valid")
        @Size(max = 200, message = "Email cannot exceed 200 characters")
        private String email;
    }
    
    @Data
    public static class PhoneUpdateRequest {
        @Pattern(regexp = "^\\d{11}$", message = "Phone must be 11 digits like 79207865432")
        private String phone;
    }
} 