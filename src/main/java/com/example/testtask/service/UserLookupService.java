package com.example.testtask.service;

import com.example.testtask.entity.User;
import com.example.testtask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserRepository userRepository;

    @Cacheable(value = "usersByEmail", key = "#email", unless="#result == null")
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Cacheable(value = "usersByPhone", key = "#phone", unless="#result == null")
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }
} 