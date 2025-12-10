package com.velocityx.wallet_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocityx.wallet_service.dto.event.WalletEvent;
import com.velocityx.wallet_service.entity.Wallet;
import com.velocityx.wallet_service.entity.WalletTransaction;
import com.velocityx.wallet_service.enums.TransactionStatus;
import com.velocityx.wallet_service.enums.TransactionType;
import com.velocityx.wallet_service.enums.WalletStatus;
import com.velocityx.wallet_service.repository.WalletRepository;
import com.velocityx.wallet_service.repository.WalletTransactionRepository;
import com.velocityx.wallet_service.service.LedgerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventConsumer {
    
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    @Value("${kafka.topics.wallet-dlq}")
    private String dlqTopic;
    
    @Value("${wallet.ledger.system-account-id:SYSTEM_ACCOUNT}")
    private String systemAccountId;
    
    @Value("${wallet.ledger.platform-account-id:PLATFORM_RESERVE}")
    private String platformAccountId;
    
    @KafkaListener(
            topics = {"${kafka.topics.wallet-events}"},
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeWalletEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            WalletEvent event = objectMapper.convertValue(payload, WalletEvent.class);
            log.info("Consuming wallet event: type={}, txnId={}", event.getEventType(), event.getTransactionId());
            
            processEvent(event);
            
            acknowledgment.acknowledge();
            log.info("Event processed successfully: {}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error processing wallet event: key={}", key, e);
            sendToDlq(payload, e.getMessage());
            acknowledgment.acknowledge();
        }
    }
    
    private void processEvent(WalletEvent event) {
        switch (event.getEventType()) {
            case CREDIT_REQUESTED -> processCredit(event);
            case DEBIT_REQUESTED -> processDebit(event);
            case HOLD_REQUESTED -> processHold(event);
            case CAPTURE_REQUESTED -> processCapture(event);
            case RELEASE_REQUESTED -> processRelease(event);
            case TRANSFER_REQUESTED -> processTransfer(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }
    
    private void processCredit(WalletEvent event) {
        WalletTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            Wallet wallet = walletRepository.findByWalletIdForUpdate(event.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            Wallet systemWallet = getOrCreateSystemWallet();
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            ledgerService.createCreditEntries(transaction, wallet, systemWallet);
            
            transaction.complete();
            transactionRepository.save(transaction);
            
            incrementMetric("wallet.credit.completed");
            log.info("Credit completed: txnId={}, amount={}", transaction.getTransactionId(), transaction.getAmount());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("wallet.credit.failed");
            throw e;
        }
    }
    
    private void processDebit(WalletEvent event) {
        WalletTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            Wallet wallet = walletRepository.findByWalletIdForUpdate(event.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            if (!wallet.hasAvailableBalance(event.getAmount())) {
                throw new RuntimeException("Insufficient funds");
            }
            
            Wallet systemWallet = getOrCreateSystemWallet();
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            ledgerService.createDebitEntries(transaction, wallet, systemWallet);
            
            transaction.complete();
            transactionRepository.save(transaction);
            
            incrementMetric("wallet.debit.completed");
            log.info("Debit completed: txnId={}, amount={}", transaction.getTransactionId(), transaction.getAmount());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("wallet.debit.failed");
            throw e;
        }
    }
    
    private void processHold(WalletEvent event) {
        WalletTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            Wallet wallet = walletRepository.findByWalletIdForUpdate(event.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            if (!wallet.hasAvailableBalance(event.getAmount())) {
                throw new RuntimeException("Insufficient funds for hold");
            }
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            ledgerService.createHoldEntries(transaction, wallet);
            
            transaction.setStatus(TransactionStatus.HELD);
            transactionRepository.save(transaction);
            
            incrementMetric("wallet.hold.completed");
            log.info("Hold completed: txnId={}, amount={}", transaction.getTransactionId(), transaction.getAmount());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("wallet.hold.failed");
            throw e;
        }
    }
    
    private void processCapture(WalletEvent event) {
        WalletTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            WalletTransaction holdTxn = transactionRepository.findByTransactionId(event.getHoldTransactionId())
                    .orElseThrow(() -> new RuntimeException("Hold transaction not found"));
            
            Wallet wallet = walletRepository.findByWalletIdForUpdate(event.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            Wallet platformWallet = getOrCreatePlatformWallet();
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            ledgerService.createCaptureEntries(transaction, wallet, platformWallet);
            
            transaction.complete();
            holdTxn.setStatus(TransactionStatus.CAPTURED);
            
            transactionRepository.save(transaction);
            transactionRepository.save(holdTxn);
            
            incrementMetric("wallet.capture.completed");
            log.info("Capture completed: txnId={}", transaction.getTransactionId());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("wallet.capture.failed");
            throw e;
        }
    }
    
    private void processRelease(WalletEvent event) {
        WalletTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            WalletTransaction holdTxn = transactionRepository.findByTransactionId(event.getHoldTransactionId())
                    .orElseThrow(() -> new RuntimeException("Hold transaction not found"));
            
            Wallet wallet = walletRepository.findByWalletIdForUpdate(event.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            ledgerService.createReleaseEntries(transaction, wallet);
            
            transaction.complete();
            holdTxn.setStatus(TransactionStatus.RELEASED);
            
            transactionRepository.save(transaction);
            transactionRepository.save(holdTxn);
            
            incrementMetric("wallet.release.completed");
            log.info("Release completed: txnId={}", transaction.getTransactionId());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("wallet.release.failed");
            throw e;
        }
    }
    
    private void processTransfer(WalletEvent event) {
        WalletTransaction senderTxn = getTransaction(event.getTransactionId());
        
        if (senderTxn.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", senderTxn.getTransactionId());
            return;
        }
        
        try {
            Wallet senderWallet = walletRepository.findByWalletIdForUpdate(event.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Sender wallet not found"));
            
            Wallet receiverWallet = walletRepository.findByWalletIdForUpdate(event.getCounterpartyWalletId())
                    .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));
            
            if (!senderWallet.hasAvailableBalance(event.getAmount())) {
                throw new RuntimeException("Insufficient funds for transfer");
            }
            
            WalletTransaction receiverTxn = WalletTransaction.builder()
                    .transactionId("TXN-" + UUID.randomUUID().toString().toUpperCase().substring(0, 16))
                    .idempotencyKey("TRANSFER-IN-" + senderTxn.getIdempotencyKey())
                    .walletId(receiverWallet.getWalletId())
                    .userId(event.getCounterpartyUserId())
                    .transactionType(TransactionType.TRANSFER_IN)
                    .status(TransactionStatus.PENDING)
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .description("Transfer from: " + senderWallet.getWalletId())
                    .counterpartyWalletId(senderWallet.getWalletId())
                    .build();
            
            receiverTxn = transactionRepository.save(receiverTxn);
            
            senderTxn.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(senderTxn);
            
            ledgerService.createTransferEntries(senderTxn, receiverTxn, senderWallet, receiverWallet);
            
            senderTxn.complete();
            receiverTxn.complete();
            
            transactionRepository.save(senderTxn);
            transactionRepository.save(receiverTxn);
            
            incrementMetric("wallet.transfer.completed");
            log.info("Transfer completed: sender={}, receiver={}", 
                    senderTxn.getTransactionId(), receiverTxn.getTransactionId());
            
        } catch (Exception e) {
            senderTxn.fail(e.getMessage());
            transactionRepository.save(senderTxn);
            incrementMetric("wallet.transfer.failed");
            throw e;
        }
    }
    
    private WalletTransaction getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }
    
    private Wallet getOrCreateSystemWallet() {
        return walletRepository.findByWalletId(systemAccountId)
                .orElseGet(() -> {
                    Wallet wallet = Wallet.builder()
                            .walletId(systemAccountId)
                            .userId(0L)
                            .currency("INR")
                            .balance(new BigDecimal("1000000000"))
                            .availableBalance(new BigDecimal("1000000000"))
                            .status(WalletStatus.ACTIVE)
                            .build();
                    return walletRepository.save(wallet);
                });
    }
    
    private Wallet getOrCreatePlatformWallet() {
        return walletRepository.findByWalletId(platformAccountId)
                .orElseGet(() -> {
                    Wallet wallet = Wallet.builder()
                            .walletId(platformAccountId)
                            .userId(0L)
                            .currency("INR")
                            .balance(BigDecimal.ZERO)
                            .availableBalance(BigDecimal.ZERO)
                            .status(WalletStatus.ACTIVE)
                            .build();
                    return walletRepository.save(wallet);
                });
    }
    
    private void sendToDlq(Map<String, Object> payload, String errorMessage) {
        payload.put("errorMessage", errorMessage);
        kafkaTemplate.send(dlqTopic, payload);
        incrementMetric("wallet.dlq.size");
    }
    
    private void incrementMetric(String name) {
        Counter.builder(name).register(meterRegistry).increment();
    }
}
