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

    @Scheduled(fixedRate = 5000)
    public void increaseBalances() {
        log.debug("Starting scheduled balance increase");

        List<Account> accounts = accountRepository.findAll();
        for (Account account : accounts) {
            accountService.increaseBalance(account);
        }
        log.debug("Finished scheduled balance increase");
    }
}
