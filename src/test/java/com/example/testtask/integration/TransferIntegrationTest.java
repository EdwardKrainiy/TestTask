package com.example.testtask.integration;

import com.example.testtask.dto.AuthRequest;
import com.example.testtask.dto.AuthResponse;
import com.example.testtask.dto.TransferRequest;
import com.example.testtask.dto.UserCreateRequest;
import com.example.testtask.dto.UserResponse;
import com.example.testtask.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class TransferIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Test
    void transferMoney_Success() throws Exception {
        UserResponse user1 = createUser("User1", "user1@transfer.com", "79200000101");
        UserResponse user2 = createUser("User2", "user2@transfer.com", "79200000102");
        
        String token = authenticateUser("user1@transfer.com", "password123");

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setTransferTo(user2.getId());
        transferRequest.setAmount(BigDecimal.valueOf(50));

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + user1.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50));

        mockMvc.perform(get("/api/users/" + user2.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150));
    }

    @Test
    void transferMoney_InsufficientBalance_ShouldFail() throws Exception {
        UserResponse user1 = createUser("User1", "user1@insufficient.com", "79200000111");
        UserResponse user2 = createUser("User2", "user2@insufficient.com", "79200000112");
        
        String token = authenticateUser("user1@insufficient.com", "password123");

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setTransferTo(user2.getId());
        transferRequest.setAmount(BigDecimal.valueOf(150));

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferMoney_ToSameUser_ShouldFail() throws Exception {
        UserResponse user = createUser("User", "user@same.com", "79200000121");
        
        String token = authenticateUser("user@same.com", "password123");

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setTransferTo(user.getId());
        transferRequest.setAmount(BigDecimal.valueOf(50));

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferMoney_InvalidAmount_ShouldFail() throws Exception {
        UserResponse user2 = createUser("User2", "user2@invalid.com", "79200000132");
        
        String token = authenticateUser("user1@invalid.com", "password123");

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setTransferTo(user2.getId());
        transferRequest.setAmount(BigDecimal.valueOf(-10));

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferMoney_UnauthorizedUser_ShouldFail() throws Exception {
        UserResponse user2 = createUser("User2", "user2@unauthorized.com", "79200000142");

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setTransferTo(user2.getId());
        transferRequest.setAmount(BigDecimal.valueOf(50));

        mockMvc.perform(post("/api/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferMoney_NonExistentRecipient_ShouldFail() throws Exception {
        UserResponse user = createUser("User", "user@nonexistent.com", "79200000151");
        
        String token = authenticateUser("user@nonexistent.com", "password123");

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setTransferTo(99999L);
        transferRequest.setAmount(BigDecimal.valueOf(50));

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());
    }

    private UserResponse createUser(String name, String email, String phone) {
        UserCreateRequest req = new UserCreateRequest();
        req.setName(name);
        req.setDateOfBirth(LocalDate.of(1990, 1, 1));
        req.setPassword("password123");
        req.setInitialBalance(BigDecimal.valueOf(100));

        List<UserCreateRequest.EmailRequest> emails = new ArrayList<>();
        UserCreateRequest.EmailRequest e = new UserCreateRequest.EmailRequest();
        e.setEmail(email);
        emails.add(e);
        req.setEmails(emails);

        List<UserCreateRequest.PhoneRequest> phones = new ArrayList<>();
        UserCreateRequest.PhoneRequest p = new UserCreateRequest.PhoneRequest();
        p.setPhone(phone);
        phones.add(p);
        req.setPhones(phones);

        return userService.createUser(req);
    }

    private String authenticateUser(String email, String password) throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setLogin(email);
        authRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                AuthResponse.class
        );

        return authResponse.getToken();
    }
} 