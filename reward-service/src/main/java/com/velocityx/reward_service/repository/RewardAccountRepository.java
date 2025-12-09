package com.velocityx.reward_service.repository;

import com.velocityx.reward_service.entity.RewardAccount;
import com.velocityx.reward_service.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface RewardAccountRepository extends JpaRepository<RewardAccount, Long> {
    
    Optional<RewardAccount> findByAccountId(String accountId);
    
    Optional<RewardAccount> findByUserId(Long userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM RewardAccount a WHERE a.accountId = :accountId")
    Optional<RewardAccount> findByAccountIdForUpdate(@Param("accountId") String accountId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM RewardAccount a WHERE a.userId = :userId")
    Optional<RewardAccount> findByUserIdForUpdate(@Param("userId") Long userId);
    
    Optional<RewardAccount> findByAccountType(AccountType accountType);
    
    boolean existsByUserId(Long userId);
}
