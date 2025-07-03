package com.example.testtask.integration;

import com.example.testtask.dto.UserCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
@Transactional
class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUser_Success() throws Exception {
        // Given
        UserCreateRequest request = new UserCreateRequest();
        request.setName("John Doe");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setPassword("password123");
        request.setInitialBalance(BigDecimal.valueOf(1000));

        UserCreateRequest.EmailRequest emailRequest = new UserCreateRequest.EmailRequest();
        emailRequest.setEmail("john@example.com");
        request.setEmails(List.of(emailRequest));

        UserCreateRequest.PhoneRequest phoneRequest = new UserCreateRequest.PhoneRequest();
        phoneRequest.setPhone("79201234567");
        request.setPhones(List.of(phoneRequest));

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.balance").value(1000))
                .andExpect(jsonPath("$.emails[0]").value("john@example.com"))
                .andExpect(jsonPath("$.phones[0]").value("79201234567"));
    }
} 