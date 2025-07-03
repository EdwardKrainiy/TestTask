package com.example.testtask.integration;

import com.example.testtask.dto.UserCreateRequest;
import com.example.testtask.dto.UserResponse;
import com.example.testtask.dto.UserUpdateRequest;
import com.example.testtask.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
    "spring.cache.type=redis"
})
class CacheTTLTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Test 
    void testCacheBasicFunctionality() {
        UserResponse created = createUser("TTLUser", "ttl@test.com", "79200000003");
        Long id = created.getId();

        String name1 = userService.getUserById(id).orElseThrow().getName();
        assertEquals("TTLUser", name1, "Initial name should be retrieved correctly");

        String name2 = userService.getUserById(id).orElseThrow().getName();
        assertEquals("TTLUser", name2, "Should return cached value");
    }

    @Test
    void testCacheEvictionOnUserUpdate() {
        UserResponse created = createUser("CacheEvictUser", "evict@test.com", "79200000004");
        Long id = created.getId();

        String name1 = userService.getUserById(id).orElseThrow().getName();
        assertEquals("CacheEvictUser", name1);

        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setName("CacheEvictUserUpdated");
        userService.updateUser(id, updateRequest);

        String name2 = userService.getUserById(id).orElseThrow().getName();
        assertEquals("CacheEvictUserUpdated", name2, "Should return updated value after service update");
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
} 