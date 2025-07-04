package com.example.testtask.integration;

import com.example.testtask.dto.UserCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUser_ValidRequest_ShouldSucceed() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Valid User");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setPassword("password123");
        request.setInitialBalance(BigDecimal.valueOf(1000));
        
        UserCreateRequest.EmailRequest emailRequest = new UserCreateRequest.EmailRequest();
        emailRequest.setEmail("valid@example.com");
        request.setEmails(List.of(emailRequest));
        
        UserCreateRequest.PhoneRequest phoneRequest = new UserCreateRequest.PhoneRequest();
        phoneRequest.setPhone("79001234567");
        request.setPhones(List.of(phoneRequest));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Valid User"))
                .andExpect(jsonPath("$.emails[0]").value("valid@example.com"));
    }

    @Test
    void createUser_DuplicateEmail_ShouldFail() throws Exception {
        UserCreateRequest firstRequest = new UserCreateRequest();
        firstRequest.setName("First User");
        firstRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        firstRequest.setPassword("password123");
        firstRequest.setInitialBalance(BigDecimal.valueOf(1000));
        
        UserCreateRequest.EmailRequest firstEmailRequest = new UserCreateRequest.EmailRequest();
        firstEmailRequest.setEmail("unique@example.com");
        firstRequest.setEmails(List.of(firstEmailRequest));
        
        UserCreateRequest.PhoneRequest firstPhoneRequest = new UserCreateRequest.PhoneRequest();
        firstPhoneRequest.setPhone("79001234568");
        firstRequest.setPhones(List.of(firstPhoneRequest));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        UserCreateRequest duplicateRequest = new UserCreateRequest();
        duplicateRequest.setName("Second User");
        duplicateRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        duplicateRequest.setPassword("password123");
        duplicateRequest.setInitialBalance(BigDecimal.valueOf(1000));
        
        UserCreateRequest.EmailRequest duplicateEmailRequest = new UserCreateRequest.EmailRequest();
        duplicateEmailRequest.setEmail("unique@example.com");
        duplicateRequest.setEmails(List.of(duplicateEmailRequest));
        
        UserCreateRequest.PhoneRequest duplicatePhoneRequest = new UserCreateRequest.PhoneRequest();
        duplicatePhoneRequest.setPhone("79001234569");
        duplicateRequest.setPhones(List.of(duplicatePhoneRequest));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_InvalidEmail_ShouldFail() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Invalid Email User");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setPassword("password123");
        request.setInitialBalance(BigDecimal.valueOf(1000));
        
        UserCreateRequest.EmailRequest emailRequest = new UserCreateRequest.EmailRequest();
        emailRequest.setEmail("invalid-email");
        request.setEmails(List.of(emailRequest));
        
        UserCreateRequest.PhoneRequest phoneRequest = new UserCreateRequest.PhoneRequest();
        phoneRequest.setPhone("79001234570");
        request.setPhones(List.of(phoneRequest));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 