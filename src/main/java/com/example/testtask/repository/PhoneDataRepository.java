package com.example.testtask.repository;

import com.example.testtask.entity.PhoneData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneDataRepository extends JpaRepository<PhoneData, Long> {
    
    List<PhoneData> findByUserId(Long userId);
    
    Optional<PhoneData> findByPhone(String phone);
    
    boolean existsByPhone(String phone);
    
    void deleteByUserId(Long userId);
} 