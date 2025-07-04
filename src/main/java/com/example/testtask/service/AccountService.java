package com.example.testtask.service;

import com.example.testtask.dto.TransferRequest;
import com.example.testtask.entity.Account;
import com.example.testtask.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    
    private final AccountRepository accountRepository;
    
    @Cacheable(value = "accounts", key = "#userId")
    public Optional<Account> getAccountByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = "accounts", allEntries = true)
    public void transferMoney(Long fromUserId, TransferRequest request) {
        log.debug("Transferring {} from user {} to user {}", 
                 request.getAmount(), fromUserId, request.getTransferTo());
        
        if (fromUserId.equals(request.getTransferTo())) {
            throw new IllegalArgumentException("Cannot transfer money to yourself");
        }
        
        // Get accounts with locking to prevent race conditions
        Optional<Account> fromAccountOpt = accountRepository.findByUserIdWithLock(fromUserId);
        Optional<Account> toAccountOpt = accountRepository.findByUserIdWithLock(request.getTransferTo());
        
        if (fromAccountOpt.isEmpty()) {
            throw new IllegalArgumentException("Source account not found");
        }
        
        if (toAccountOpt.isEmpty()) {
            throw new IllegalArgumentException("Destination account not found");
        }
        
        Account fromAccount = fromAccountOpt.get();
        Account toAccount = toAccountOpt.get();
        
        // Check if sender has enough balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        
        // Perform transfer
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(request.getAmount());
        BigDecimal newToBalance = toAccount.getBalance().add(request.getAmount());
        
        // Ensure balance doesn't go negative
        if (newFromBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transfer would result in negative balance");
        }
        
        // Update balances
        fromAccount.setBalance(newFromBalance);
        toAccount.setBalance(newToBalance);
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        log.info("Transfer completed successfully: {} from user {} to user {}", 
                request.getAmount(), fromUserId, request.getTransferTo());
    }
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public void increaseBalances() {
        log.debug("Starting scheduled balance increase");
        
        List<Account> accounts = accountRepository.findAll();
        
        for (Account account : accounts) {
            BigDecimal currentBalance = account.getBalance();
            BigDecimal initialBalance = account.getInitialBalance();
            BigDecimal maxBalance = initialBalance.multiply(BigDecimal.valueOf(2.07)); // 207% of initial
            
            // Calculate 10% increase
            BigDecimal increase = currentBalance.multiply(BigDecimal.valueOf(0.10))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal newBalance = currentBalance.add(increase);
            
            // Don't exceed 207% of initial balance
            if (newBalance.compareTo(maxBalance) > 0) {
                newBalance = maxBalance;
            }
            
            // Only update if there's an actual change
            if (newBalance.compareTo(currentBalance) > 0) {
                account.setBalance(newBalance);
                accountRepository.save(account);
                
                log.debug("Increased balance for user {}: {} -> {}", 
                         account.getUserId(), currentBalance, newBalance);
            }
        }
        
        log.info("Completed scheduled balance increase for {} accounts", accounts.size());
    }
} 