package com.example.testtask.config;

import com.example.testtask.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.scheduled-tasks.enabled", matchIfMissing = true)
public class ScheduledTasksConfig {

    private final AccountService accountService;

    @Scheduled(fixedRate = 30000)
    public void increaseBalances() {
        accountService.increaseBalances();
    }
} 