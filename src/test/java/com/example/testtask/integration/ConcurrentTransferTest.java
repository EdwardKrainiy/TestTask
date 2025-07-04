package com.example.testtask.integration;

import com.example.testtask.dto.TransferRequest;
import com.example.testtask.dto.UserCreateRequest;
import com.example.testtask.dto.UserResponse;
import com.example.testtask.service.AccountService;
import com.example.testtask.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConcurrentTransferTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Test
    void testConcurrentTransfers() throws InterruptedException {
        // create two users
        long user1 = createUser("User1", "user1@test.com", "79200000001").getId();
        long user2 = createUser("User2", "user2@test.com", "79200000002").getId();

        int threads = 10;
        BigDecimal transferAmount = BigDecimal.valueOf(5);
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    TransferRequest req = new TransferRequest();
                    req.setTransferTo(user2);
                    req.setAmount(transferAmount);
                    accountService.transferMoney(user1, req);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        BigDecimal expectedUser1 = BigDecimal.valueOf(100).subtract(transferAmount.multiply(BigDecimal.valueOf(threads)));
        BigDecimal expectedUser2 = BigDecimal.valueOf(100).add(transferAmount.multiply(BigDecimal.valueOf(threads)));

        BigDecimal actual1 = accountService.getAccountByUserId(user1).orElseThrow().getBalance();
        BigDecimal actual2 = accountService.getAccountByUserId(user2).orElseThrow().getBalance();

        Assertions.assertEquals(0, expectedUser1.compareTo(actual1), "User1 balance mismatch");
        Assertions.assertEquals(0, expectedUser2.compareTo(actual2), "User2 balance mismatch");
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