package com.example.testtask.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UserCreateRequest {
    
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 500, message = "Name cannot exceed 500 characters")
    private String name;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 500, message = "Password must be between 8 and 500 characters")
    private String password;
    
    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
    private BigDecimal initialBalance;
    
    @NotEmpty(message = "At least one email is required")
    @Valid
    private List<EmailRequest> emails;
    
    @NotEmpty(message = "At least one phone is required")
    @Valid
    private List<PhoneRequest> phones;
    
    @Data
    public static class EmailRequest {
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        @Size(max = 200, message = "Email cannot exceed 200 characters")
        private String email;
    }
    
    @Data
    public static class PhoneRequest {
        @NotBlank(message = "Phone cannot be blank")
        @Pattern(regexp = "^\\d{11}$", message = "Phone must be 11 digits like 79207865432")
        private String phone;
    }
} 