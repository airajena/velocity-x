package com.velocityx.reward_service.repository;

import com.velocityx.reward_service.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    
    List<LedgerEntry> findByAccountId(String accountId);
    
    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);
}
