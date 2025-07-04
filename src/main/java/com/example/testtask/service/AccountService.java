package com.example.testtask.service;

import com.example.testtask.dto.TransferRequest;
import com.example.testtask.entity.Account;
import com.example.testtask.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import jakarta.persistence.OptimisticLockException;

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

    @Retryable(retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public void transferMoney(Long fromUserId, TransferRequest request) {
        log.debug("Transferring {} from user {} to user {}", 
                 request.getAmount(), fromUserId, request.getTransferTo());
        
        if (fromUserId.equals(request.getTransferTo())) {
            throw new IllegalArgumentException("Cannot transfer money to yourself");
        }
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        
        Long firstUserId = fromUserId.compareTo(request.getTransferTo()) < 0 ?
                fromUserId : request.getTransferTo();
        Long secondUserId = fromUserId.compareTo(request.getTransferTo()) < 0 ? 
                request.getTransferTo() : fromUserId;
        
        Optional<Account> firstAccountOpt = accountRepository.findByUserIdWithLock(firstUserId);
        Optional<Account> secondAccountOpt = accountRepository.findByUserIdWithLock(secondUserId);
        
        if (firstAccountOpt.isEmpty() || secondAccountOpt.isEmpty()) {
            throw new IllegalArgumentException("One or both accounts not found");
        }
        
        Account fromAccount = firstUserId.equals(fromUserId) ? firstAccountOpt.get() : secondAccountOpt.get();
        Account toAccount = firstUserId.equals(fromUserId) ? secondAccountOpt.get() : firstAccountOpt.get();
        
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(request.getAmount());
        BigDecimal newToBalance = toAccount.getBalance().add(request.getAmount());
        
        if (newFromBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transfer would result in negative balance");
        }
        
        fromAccount.setBalance(newFromBalance);
        toAccount.setBalance(newToBalance);
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        log.info("Transfer completed successfully: {} from user {} to user {}", 
                request.getAmount(), fromUserId, request.getTransferTo());
    }

    @Retryable(retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public void increaseBalances() {
        log.debug("Starting scheduled balance increase");
        
        List<Account> accounts = accountRepository.findAll();
        int updatedCount = 0;
        
        for (Account account : accounts) {
            try {
                BigDecimal currentBalance = account.getBalance();
                BigDecimal initialBalance = account.getInitialBalance();
                BigDecimal maxBalance = initialBalance.multiply(BigDecimal.valueOf(2.07));
                
                BigDecimal increase = currentBalance.multiply(BigDecimal.valueOf(0.10))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal newBalance = currentBalance.add(increase);
                
                if (newBalance.compareTo(maxBalance) > 0) {
                    newBalance = maxBalance;
                }
                
                if (newBalance.compareTo(currentBalance) > 0) {
                    account.setBalance(newBalance);
                    accountRepository.save(account);
                    updatedCount++;
                    
                    log.debug("Increased balance for user {}: {} -> {}", 
                             account.getUserId(), currentBalance, newBalance);
                }
            } catch (Exception e) {
                log.warn("Failed to update balance for account {}: {}", 
                        account.getId(), e.getMessage());
            }
        }
        
        log.info("Completed scheduled balance increase. Updated {} out of {} accounts", 
                updatedCount, accounts.size());
    }
} 