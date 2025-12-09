package com.velocityx.reward_service.service;

import com.velocityx.reward_service.entity.LedgerEntry;
import com.velocityx.reward_service.entity.RewardAccount;
import com.velocityx.reward_service.entity.RewardTransaction;
import com.velocityx.reward_service.enums.LedgerEntryType;
import com.velocityx.reward_service.repository.LedgerEntryRepository;
import com.velocityx.reward_service.repository.RewardAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {
    
    private final RewardAccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    
    @Transactional
    public void createEarnEntries(RewardTransaction transaction, RewardAccount userAccount, RewardAccount systemAccount) {
        BigDecimal amount = transaction.getAmount();
        
        LedgerEntry userCredit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .accountId(userAccount.getAccountId())
                .entryType(LedgerEntryType.CREDIT)
                .amount(amount)
                .balanceBefore(userAccount.getBalanceAvailable())
                .balanceAfter(userAccount.getBalanceAvailable().add(amount))
                .description("Earn: " + transaction.getDescription())
                .build();
        
        LedgerEntry systemDebit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .accountId(systemAccount.getAccountId())
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount)
                .balanceBefore(systemAccount.getBalanceAvailable())
                .balanceAfter(systemAccount.getBalanceAvailable().subtract(amount))
                .description("System debit for earn: " + transaction.getTransactionId())
                .build();
        
        transaction.addLedgerEntry(userCredit);
        transaction.addLedgerEntry(systemDebit);
        
        userAccount.creditAvailable(amount);
        accountRepository.save(userAccount);
        
        log.info("Created earn ledger entries: transactionId={}, amount={}", 
                transaction.getTransactionId(), amount);
    }
    
    @Transactional
    public void createHoldEntries(RewardTransaction transaction, RewardAccount userAccount) {
        BigDecimal amount = transaction.getAmount();
        
        LedgerEntry availableDebit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .accountId(userAccount.getAccountId())
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount)
                .balanceBefore(userAccount.getBalanceAvailable())
                .balanceAfter(userAccount.getBalanceAvailable().subtract(amount))
                .description("Hold: Available to Reserved")
                .build();
        
        LedgerEntry reservedCredit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .accountId(userAccount.getAccountId() + "_RESERVED")
                .entryType(LedgerEntryType.CREDIT)
                .amount(amount)
                .balanceBefore(userAccount.getBalanceReserved())
                .balanceAfter(userAccount.getBalanceReserved().add(amount))
                .description("Hold: Reserved credit")
                .build();
        
        transaction.addLedgerEntry(availableDebit);
        transaction.addLedgerEntry(reservedCredit);
        
        userAccount.holdFunds(amount);
        accountRepository.save(userAccount);
        
        log.info("Created hold ledger entries: transactionId={}, amount={}", 
                transaction.getTransactionId(), amount);
    }
    
    @Transactional
    public void createCaptureEntries(RewardTransaction transaction, RewardAccount userAccount, RewardAccount platformAccount) {
        BigDecimal amount = transaction.getAmount();
        
        LedgerEntry reservedDebit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .accountId(userAccount.getAccountId() + "_RESERVED")
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount)
                .balanceBefore(userAccount.getBalanceReserved())
                .balanceAfter(userAccount.getBalanceReserved().subtract(amount))
                .description("Capture: Reserved debit")
                .build();
        
        LedgerEntry platformCredit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .accountId(platformAccount.getAccountId())
                .entryType(LedgerEntryType.CREDIT)
                .amount(amount)
                .balanceBefore(platformAccount.getBalanceAvailable())
                .balanceAfter(platformAccount.getBalanceAvailable().add(amount))
                .description("Capture: Platform credit")
                .build();
        
        transaction.addLedgerEntry(reservedDebit);
        transaction.addLedgerEntry(platformCredit);
        
        userAccount.captureHold(amount);
        platformAccount.creditAvailable(amount);
        
        accountRepository.save(userAccount);
        accountRepository.save(platformAccount);
        
        log.info("Created capture ledger entries: transactionId={}, amount={}", 
                transaction.getTransactionId(), amount);
    }
    
    @Transactional
    public void createReleaseEntries(RewardTransaction transaction, RewardAccount userAccount) {
        BigDecimal amount = transaction.getAmount();
        
        LedgerEntry reservedDebit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .accountId(userAccount.getAccountId() + "_RESERVED")
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount)
                .balanceBefore(userAccount.getBalanceReserved())
                .balanceAfter(userAccount.getBalanceReserved().subtract(amount))
                .description("Release: Reserved debit")
                .build();
        
        LedgerEntry availableCredit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .accountId(userAccount.getAccountId())
                .entryType(LedgerEntryType.CREDIT)
                .amount(amount)
                .balanceBefore(userAccount.getBalanceAvailable())
                .balanceAfter(userAccount.getBalanceAvailable().add(amount))
                .description("Release: Available credit")
                .build();
        
        transaction.addLedgerEntry(reservedDebit);
        transaction.addLedgerEntry(availableCredit);
        
        userAccount.releaseHold(amount);
        accountRepository.save(userAccount);
        
        log.info("Created release ledger entries: transactionId={}, amount={}", 
                transaction.getTransactionId(), amount);
    }
    
    private String generateEntryId() {
        return "LED-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
    }
}
