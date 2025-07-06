package com.example.testtask.service;

import com.example.testtask.dto.TransferRequest;
import com.example.testtask.entity.Account;
import com.example.testtask.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.OptimisticLockException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    
    private final AccountRepository accountRepository;
    
    public Optional<Account> getAccountByUserId(Long userId) {
        log.debug("Fetching account for user ID: {}", userId);
        return accountRepository.findByUserId(userId);
    }

    @Retryable(
        retryFor = {
            ObjectOptimisticLockingFailureException.class, 
            OptimisticLockException.class, 
            StaleObjectStateException.class,
            OptimisticLockingFailureException.class
        },
        maxAttempts = 5,
        backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 2000)
    )
    @Transactional
    public void transferMoney(Long fromUserId, TransferRequest request) {
        Long toUserId = request.getTransferTo();
        BigDecimal transferAmount = request.getAmount();
        
        log.info("Initiating transfer: {} from user {} to user {}", 
                 transferAmount, fromUserId, toUserId);
        
        if (fromUserId.equals(toUserId)) {
            log.warn("Transfer rejected: cannot transfer to yourself. User ID: {}", fromUserId);
            throw new IllegalArgumentException("Cannot transfer money to yourself");
        }
        
        if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Transfer rejected: invalid amount {}. User ID: {}", transferAmount, fromUserId);
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        
        Optional<Account> fromAccountOpt = accountRepository.findByUserId(fromUserId);
        Optional<Account> toAccountOpt = accountRepository.findByUserId(toUserId);
        
        if (fromAccountOpt.isEmpty()) {
            log.warn("Transfer rejected: from account not found. User ID: {}", fromUserId);
            throw new IllegalArgumentException("Source account not found");
        }
        
        if (toAccountOpt.isEmpty()) {
            log.warn("Transfer rejected: to account not found. User ID: {}", toUserId);
            throw new IllegalArgumentException("Destination account not found");
        }
        
        Account fromAccount = fromAccountOpt.get();
        Account toAccount = toAccountOpt.get();
        
        log.debug("Transfer processing: from account version={}, to account version={}", 
                 fromAccount.getVersion(), toAccount.getVersion());
        
        if (fromAccount.getBalance().compareTo(transferAmount) < 0) {
            log.warn("Transfer rejected: insufficient balance. User ID: {}, balance: {}, requested: {}", 
                    fromUserId, fromAccount.getBalance(), transferAmount);
            throw new IllegalArgumentException("Insufficient balance");
        }
        
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(transferAmount);
        BigDecimal newToBalance = toAccount.getBalance().add(transferAmount);
        
        if (newFromBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Transfer rejected: would result in negative balance. User ID: {}, new balance: {}", 
                    fromUserId, newFromBalance);
            throw new IllegalArgumentException("Transfer would result in negative balance");
        }
        
        fromAccount.setBalance(newFromBalance);
        toAccount.setBalance(newToBalance);
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        log.info("Transfer completed successfully: {} from user {} to user {}. From balance: {} -> {}, To balance: {} -> {}", 
                transferAmount, fromUserId, toUserId, 
                fromAccount.getBalance().add(transferAmount), newFromBalance,
                toAccount.getBalance().subtract(transferAmount), newToBalance);
    }

    @Retryable(
        retryFor = {
            ObjectOptimisticLockingFailureException.class, 
            OptimisticLockException.class, 
            StaleObjectStateException.class,
            OptimisticLockingFailureException.class
        },
        maxAttempts = 5,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 500)
    )
    @Transactional
    @Async(value = "schedulerExecutor")
    public void increaseBalance(Account account) {
        try {
            Optional<Account> currentAccountOpt = accountRepository.findByUserId(account.getUserId());
            if (currentAccountOpt.isEmpty()) {
                log.warn("Account not found for balance increase. User ID: {}", account.getUserId());
                return;
            }
            
            Account currentAccount = currentAccountOpt.get();
            BigDecimal currentBalance = currentAccount.getBalance();
            BigDecimal initialBalance = currentAccount.getInitialBalance();
            
            BigDecimal maxBalance = initialBalance.multiply(BigDecimal.valueOf(2.07));
            
            if (currentBalance.compareTo(maxBalance) >= 0) {
                log.debug("Balance already at maximum for user {}: current={}, max={}", 
                        account.getUserId(), currentBalance, maxBalance);
                return;
            }
            
            BigDecimal increase = currentBalance.multiply(BigDecimal.valueOf(0.10))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal newBalance = currentBalance.add(increase);
            
            if (newBalance.compareTo(maxBalance) > 0) {
                newBalance = maxBalance;
                increase = maxBalance.subtract(currentBalance);
            }
            
            if (increase.compareTo(BigDecimal.ZERO) > 0) {
                currentAccount.setBalance(newBalance);
                accountRepository.save(currentAccount);
                
                log.info("Balance increased for user {}: {} -> {} (increase: {})", 
                        account.getUserId(), currentBalance, newBalance, increase);
            }
            
        } catch (Exception e) {
            log.error("Error increasing balance for user {}: {}", account.getUserId(), e.getMessage());
            throw e;
        }
    }
} 