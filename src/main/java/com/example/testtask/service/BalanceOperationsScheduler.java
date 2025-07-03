package com.example.testtask.service;

import com.example.testtask.entity.Account;
import com.example.testtask.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceOperationsScheduler {

    private final AccountRepository accountRepository;
    private final AccountService accountService;

    @Scheduled(fixedRate = 30000)
    public void increaseBalances() {
        log.info("Starting scheduled balance increase for all accounts");
        
        try {
            List<Account> accounts = accountRepository.findAll();
            log.debug("Found {} accounts for balance increase", accounts.size());
            
            for (Account account : accounts) {
                try {
                    accountService.increaseBalance(account);
                } catch (Exception e) {
                    log.error("Failed to increase balance for user {}: {}", 
                            account.getUserId(), e.getMessage());
                }
            }
            
            log.info("Completed scheduled balance increase for {} accounts", accounts.size());
        } catch (Exception e) {
            log.error("Error during scheduled balance increase: {}", e.getMessage(), e);
        }
    }
}
