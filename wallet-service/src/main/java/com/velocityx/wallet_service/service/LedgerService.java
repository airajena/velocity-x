package com.velocityx.wallet_service.service;

import com.velocityx.wallet_service.entity.LedgerEntry;
import com.velocityx.wallet_service.entity.Wallet;
import com.velocityx.wallet_service.entity.WalletTransaction;
import com.velocityx.wallet_service.enums.LedgerEntryType;
import com.velocityx.wallet_service.repository.LedgerEntryRepository;
import com.velocityx.wallet_service.repository.WalletRepository;
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
    
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    
    @Transactional
    public void createCreditEntries(WalletTransaction transaction, Wallet wallet, Wallet systemWallet) {
        BigDecimal amount = transaction.getAmount();
        
        LedgerEntry walletCredit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .walletId(wallet.getWalletId())
                .entryType(LedgerEntryType.CREDIT)
                .entrySide("CR")
                .amount(amount)
                .balanceBefore(wallet.getBalance())
                .balanceAfter(wallet.getBalance().add(amount))
                .description("Credit: " + transaction.getDescription())
                .build();
        
        LedgerEntry systemDebit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .walletId(systemWallet.getWalletId())
                .entryType(LedgerEntryType.DEBIT)
                .entrySide("DR")
                .amount(amount)
                .balanceBefore(systemWallet.getBalance())
                .balanceAfter(systemWallet.getBalance().subtract(amount))
                .description("System debit for: " + transaction.getTransactionId())
                .build();
        
        transaction.addLedgerEntry(walletCredit);
        transaction.addLedgerEntry(systemDebit);
        
        transaction.setBalanceBefore(wallet.getBalance());
        wallet.credit(amount);
        transaction.setBalanceAfter(wallet.getBalance());
        
        walletRepository.save(wallet);
        
        log.info("Created credit ledger entries: txnId={}, amount={}", transaction.getTransactionId(), amount);
    }
    
    @Transactional
    public void createDebitEntries(WalletTransaction transaction, Wallet wallet, Wallet systemWallet) {
        BigDecimal amount = transaction.getAmount();
        
        LedgerEntry walletDebit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .walletId(wallet.getWalletId())
                .entryType(LedgerEntryType.DEBIT)
                .entrySide("DR")
                .amount(amount)
                .balanceBefore(wallet.getBalance())
                .balanceAfter(wallet.getBalance().subtract(amount))
                .description("Debit: " + transaction.getDescription())
                .build();
        
        LedgerEntry systemCredit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .walletId(systemWallet.getWalletId())
                .entryType(LedgerEntryType.CREDIT)
                .entrySide("CR")
                .amount(amount)
                .balanceBefore(systemWallet.getBalance())
                .balanceAfter(systemWallet.getBalance().add(amount))
                .description("System credit for: " + transaction.getTransactionId())
                .build();
        
        transaction.addLedgerEntry(walletDebit);
        transaction.addLedgerEntry(systemCredit);
        
        transaction.setBalanceBefore(wallet.getBalance());
        wallet.debit(amount);
        transaction.setBalanceAfter(wallet.getBalance());
        
        walletRepository.save(wallet);
        
        log.info("Created debit ledger entries: txnId={}, amount={}", transaction.getTransactionId(), amount);
    }
    
    @Transactional
    public void createHoldEntries(WalletTransaction transaction, Wallet wallet) {
        BigDecimal amount = transaction.getAmount();
        
        LedgerEntry availableDebit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .walletId(wallet.getWalletId())
                .entryType(LedgerEntryType.DEBIT)
                .entrySide("DR")
                .amount(amount)
                .balanceBefore(wallet.getAvailableBalance())
                .balanceAfter(wallet.getAvailableBalance().subtract(amount))
                .description("Hold: Available to Reserved")
                .build();
        
        transaction.addLedgerEntry(availableDebit);
        
        transaction.setBalanceBefore(wallet.getAvailableBalance());
        wallet.holdFunds(amount);
        transaction.setBalanceAfter(wallet.getAvailableBalance());
        
        walletRepository.save(wallet);
        
        log.info("Created hold ledger entries: txnId={}, amount={}", transaction.getTransactionId(), amount);
    }
    
    @Transactional
    public void createCaptureEntries(WalletTransaction transaction, Wallet wallet, Wallet platformWallet) {
        BigDecimal amount = transaction.getAmount();
        
        LedgerEntry heldDebit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .walletId(wallet.getWalletId())
                .entryType(LedgerEntryType.DEBIT)
                .entrySide("DR")
                .amount(amount)
                .balanceBefore(wallet.getBalance())
                .balanceAfter(wallet.getBalance().subtract(amount))
                .description("Capture: Held funds deducted")
                .build();
        
        LedgerEntry platformCredit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .walletId(platformWallet.getWalletId())
                .entryType(LedgerEntryType.CREDIT)
                .entrySide("CR")
                .amount(amount)
                .balanceBefore(platformWallet.getBalance())
                .balanceAfter(platformWallet.getBalance().add(amount))
                .description("Capture: Platform credit")
                .build();
        
        transaction.addLedgerEntry(heldDebit);
        transaction.addLedgerEntry(platformCredit);
        
        transaction.setBalanceBefore(wallet.getBalance());
        wallet.captureHold(amount);
        transaction.setBalanceAfter(wallet.getBalance());
        
        platformWallet.credit(amount);
        
        walletRepository.save(wallet);
        walletRepository.save(platformWallet);
        
        log.info("Created capture ledger entries: txnId={}, amount={}", transaction.getTransactionId(), amount);
    }
    
    @Transactional
    public void createReleaseEntries(WalletTransaction transaction, Wallet wallet) {
        BigDecimal amount = transaction.getAmount();
        
        LedgerEntry releaseCredit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(transaction)
                .walletId(wallet.getWalletId())
                .entryType(LedgerEntryType.CREDIT)
                .entrySide("CR")
                .amount(amount)
                .balanceBefore(wallet.getAvailableBalance())
                .balanceAfter(wallet.getAvailableBalance().add(amount))
                .description("Release: Hold returned to available")
                .build();
        
        transaction.addLedgerEntry(releaseCredit);
        
        transaction.setBalanceBefore(wallet.getAvailableBalance());
        wallet.releaseHold(amount);
        transaction.setBalanceAfter(wallet.getAvailableBalance());
        
        walletRepository.save(wallet);
        
        log.info("Created release ledger entries: txnId={}, amount={}", transaction.getTransactionId(), amount);
    }
    
    @Transactional
    public void createTransferEntries(WalletTransaction senderTxn, WalletTransaction receiverTxn,
                                       Wallet senderWallet, Wallet receiverWallet) {
        BigDecimal amount = senderTxn.getAmount();
        
        LedgerEntry senderDebit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(senderTxn)
                .walletId(senderWallet.getWalletId())
                .entryType(LedgerEntryType.DEBIT)
                .entrySide("DR")
                .amount(amount)
                .balanceBefore(senderWallet.getBalance())
                .balanceAfter(senderWallet.getBalance().subtract(amount))
                .description("Transfer out to: " + receiverWallet.getWalletId())
                .build();
        
        LedgerEntry receiverCredit = LedgerEntry.builder()
                .entryId(generateEntryId())
                .transaction(receiverTxn)
                .walletId(receiverWallet.getWalletId())
                .entryType(LedgerEntryType.CREDIT)
                .entrySide("CR")
                .amount(amount)
                .balanceBefore(receiverWallet.getBalance())
                .balanceAfter(receiverWallet.getBalance().add(amount))
                .description("Transfer in from: " + senderWallet.getWalletId())
                .build();
        
        senderTxn.addLedgerEntry(senderDebit);
        receiverTxn.addLedgerEntry(receiverCredit);
        
        senderTxn.setBalanceBefore(senderWallet.getBalance());
        senderWallet.debit(amount);
        senderTxn.setBalanceAfter(senderWallet.getBalance());
        
        receiverTxn.setBalanceBefore(receiverWallet.getBalance());
        receiverWallet.credit(amount);
        receiverTxn.setBalanceAfter(receiverWallet.getBalance());
        
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);
        
        log.info("Created transfer ledger entries: amount={}", amount);
    }
    
    private String generateEntryId() {
        return "LED-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
    }
}
