package com.velocityx.wallet_service.repository;

import com.velocityx.wallet_service.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    Optional<Wallet> findByWalletId(String walletId);
    
    Optional<Wallet> findByUserId(Long userId);
    
    Optional<Wallet> findByUserIdAndCurrency(Long userId, String currency);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletId = :walletId")
    Optional<Wallet> findByWalletIdForUpdate(@Param("walletId") String walletId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId AND w.currency = :currency")
    Optional<Wallet> findByUserIdAndCurrencyForUpdate(@Param("userId") Long userId, @Param("currency") String currency);
    
    boolean existsByUserId(Long userId);
    
    boolean existsByUserIdAndCurrency(Long userId, String currency);
}
