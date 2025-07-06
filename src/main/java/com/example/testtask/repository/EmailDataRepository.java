package com.example.testtask.repository;

import com.example.testtask.entity.EmailData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailDataRepository extends JpaRepository<EmailData, Long> {
    
    List<EmailData> findByUserId(Long userId);
    
    Optional<EmailData> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    void deleteByUserId(Long userId);
} 