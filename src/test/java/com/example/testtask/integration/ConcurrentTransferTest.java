package com.example.testtask.integration;

import com.example.testtask.dto.TransferRequest;
import com.example.testtask.dto.UserCreateRequest;
import com.example.testtask.dto.UserResponse;
import com.example.testtask.service.AccountService;
import com.example.testtask.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentTransferTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Test
    void testConcurrentTransfers_SameDirection() throws InterruptedException {
        long user1 = createUser("User1", "user1@concurrent.com", "79200000001").getId();
        long user2 = createUser("User2", "user2@concurrent.com", "79200000002").getId();

        int threadsCount = 5;
        BigDecimal transferAmount = BigDecimal.valueOf(10);
        CountDownLatch latch = new CountDownLatch(threadsCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                try {
                    TransferRequest req = new TransferRequest();
                    req.setTransferTo(user2);
                    req.setAmount(transferAmount);
                    accountService.transferMoney(user1, req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    synchronized (errors) {
                        errors.add(e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Transfers should complete within 30 seconds");
        executor.shutdown();

        System.out.println("Success count: " + successCount.get());
        System.out.println("Failure count: " + failureCount.get());
        if (!errors.isEmpty()) {
            System.out.println("Errors: " + errors);
        }

        BigDecimal finalUser1Balance = accountService.getAccountByUserId(user1).orElseThrow().getBalance();
        BigDecimal finalUser2Balance = accountService.getAccountByUserId(user2).orElseThrow().getBalance();
        
        System.out.println("Final User1 balance: " + finalUser1Balance);
        System.out.println("Final User2 balance: " + finalUser2Balance);
        
        BigDecimal totalMoney = finalUser1Balance.add(finalUser2Balance);
        assertEquals(0, BigDecimal.valueOf(200).compareTo(totalMoney), "Total money should remain constant");
        
        BigDecimal expectedTransferred = transferAmount.multiply(BigDecimal.valueOf(successCount.get()));
        BigDecimal actualTransferred = BigDecimal.valueOf(100).subtract(finalUser1Balance);
        assertEquals(0, expectedTransferred.compareTo(actualTransferred), 
                "Transferred amount should match successful transfers");
        
        assertTrue(successCount.get() > 0, "At least some transfers should succeed");
        assertTrue(finalUser1Balance.compareTo(BigDecimal.ZERO) >= 0, "User1 balance should not be negative");
    }

    @Test
    void testConcurrentTransfers_BothDirections() throws InterruptedException {
        long user1 = createUser("User1", "user1@bidirectional.com", "79200000011").getId();
        long user2 = createUser("User2", "user2@bidirectional.com", "79200000012").getId();

        int threadsCount = 6;
        BigDecimal transferAmount = BigDecimal.valueOf(10);
        CountDownLatch latch = new CountDownLatch(threadsCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < threadsCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    TransferRequest req = new TransferRequest();
                    req.setAmount(transferAmount);
                    
                    if (threadIndex % 2 == 0) {
                        req.setTransferTo(user2);
                        accountService.transferMoney(user1, req);
                    } else {
                        req.setTransferTo(user1);
                        accountService.transferMoney(user2, req);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    synchronized (errors) {
                        errors.add(e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Transfers should complete within 30 seconds");
        executor.shutdown();

        System.out.println("Success count: " + successCount.get());
        if (!errors.isEmpty()) {
            System.out.println("Errors: " + errors);
        }

        BigDecimal finalUser1Balance = accountService.getAccountByUserId(user1).orElseThrow().getBalance();
        BigDecimal finalUser2Balance = accountService.getAccountByUserId(user2).orElseThrow().getBalance();
        
        BigDecimal totalMoney = finalUser1Balance.add(finalUser2Balance);
        assertEquals(0, BigDecimal.valueOf(200).compareTo(totalMoney), "Total money should remain constant");
        
        assertTrue(finalUser1Balance.compareTo(BigDecimal.ZERO) >= 0, "User1 balance should not be negative");
        assertTrue(finalUser2Balance.compareTo(BigDecimal.ZERO) >= 0, "User2 balance should not be negative");
        
        assertTrue(successCount.get() > 0, "At least some transfers should succeed");
    }

    @Test
    void testTransferToSameUser_ShouldFail() {
        long userId = createUser("User", "user@same.com", "79200000020").getId();

        TransferRequest req = new TransferRequest();
        req.setTransferTo(userId);
        req.setAmount(BigDecimal.valueOf(10));
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.transferMoney(userId, req)
        );
        
        assertEquals("Cannot transfer money to yourself", exception.getMessage());
    }

    @Test
    void testTransferInsufficientBalance_ShouldFail() {
        long user1 = createUser("User1", "user1@insufficient.com", "79200000021").getId();
        long user2 = createUser("User2", "user2@insufficient.com", "79200000022").getId();

        TransferRequest req = new TransferRequest();
        req.setTransferTo(user2);
        req.setAmount(BigDecimal.valueOf(150));
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.transferMoney(user1, req)
        );
        
        assertEquals("Insufficient balance", exception.getMessage());
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