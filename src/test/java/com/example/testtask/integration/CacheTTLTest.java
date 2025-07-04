package com.example.testtask.integration;

import com.example.testtask.dto.UserCreateRequest;
import com.example.testtask.dto.UserResponse;
import com.example.testtask.entity.User;
import com.example.testtask.repository.UserRepository;
import com.example.testtask.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@TestPropertySource(properties = "app.cache.ttl-seconds=1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CacheTTLTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCacheEvictsAfterTtl() throws InterruptedException {
        UserResponse created = createUser("TTLUser", "ttl@test.com", "79200000003");
        Long id = created.getId();

        // First call caches response
        String name1 = userService.getUserById(id).orElseThrow().getName();
        Assertions.assertEquals("TTLUser", name1);

        // Update user name directly in DB (bypass cache)
        User userEntity = userRepository.findById(id).orElseThrow();
        userEntity.setName("TTLUserUpdated");
        userRepository.save(userEntity);

        // Wait for TTL to expire
        Thread.sleep(1500);

        String name2 = userService.getUserById(id).orElseThrow().getName();
        Assertions.assertEquals("TTLUserUpdated", name2);
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