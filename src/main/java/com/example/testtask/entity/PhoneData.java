package com.example.testtask.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "phone_data", uniqueConstraints = @UniqueConstraint(columnNames = "phone"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PhoneData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "^\\d{11}$", message = "Phone must be 11 digits like 79207865432")
    @Size(max = 13, message = "Phone cannot exceed 13 characters")
    @Column(name = "phone", nullable = false, length = 13, unique = true)
    private String phone;
    
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;
} 