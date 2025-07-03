package com.example.testtask.repository;

import com.example.testtask.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByUserId(Long userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.balance = :balance WHERE a.userId = :userId")
    void updateBalance(@Param("userId") Long userId, @Param("balance") BigDecimal balance);
    
    @Query("SELECT a FROM Account a WHERE a.userId = :userId")
    Optional<Account> findByUserIdWithLock(@Param("userId") Long userId);
} 