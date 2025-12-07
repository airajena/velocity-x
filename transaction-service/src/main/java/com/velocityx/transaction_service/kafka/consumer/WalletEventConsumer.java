package com.velocityx.transaction_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocityx.transaction_service.dto.event.WalletEventDTO;
import com.velocityx.transaction_service.entity.Transaction;
import com.velocityx.transaction_service.enums.TransactionStatus;
import com.velocityx.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventConsumer {
    
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
            topics = "${kafka.topics.wallet-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeWalletEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {
        
        log.info("Received wallet event: topic={}, offset={}, key={}", topic, offset, key);
        
        try {
            WalletEventDTO event = objectMapper.convertValue(payload, WalletEventDTO.class);
            
            processWalletEvent(event);
            
            acknowledgment.acknowledge();
            log.info("Wallet event processed successfully: transactionId={}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error processing wallet event: offset={}", offset, e);
            acknowledgment.acknowledge();
        }
    }
    
    private void processWalletEvent(WalletEventDTO event) {
        if (event.getTransactionId() == null) {
            log.warn("Wallet event without transactionId, skipping");
            return;
        }
        
        Optional<Transaction> transactionOpt = 
                transactionRepository.findByTransactionId(event.getTransactionId());
        
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for wallet event: transactionId={}", event.getTransactionId());
            return;
        }
        
        Transaction transaction = transactionOpt.get();
        
        switch (event.getEventType()) {
            case "WALLET_CREDITED":
                handleWalletCredited(transaction, event);
                break;
            case "WALLET_DEBITED":
                handleWalletDebited(transaction, event);
                break;
            case "WALLET_HOLD_CREATED":
                handleWalletHoldCreated(transaction, event);
                break;
            case "WALLET_HOLD_RELEASED":
                handleWalletHoldReleased(transaction, event);
                break;
            case "WALLET_INSUFFICIENT_FUNDS":
                handleInsufficientFunds(transaction, event);
                break;
            default:
                log.warn("Unknown wallet event type: {}", event.getEventType());
        }
    }
    
    private void handleWalletCredited(Transaction transaction, WalletEventDTO event) {
        log.info("Processing wallet credited event: transactionId={}", transaction.getTransactionId());
        
        if (transaction.getStatus() == TransactionStatus.PENDING) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(Instant.now());
            transactionRepository.save(transaction);
            log.info("Transaction marked as SUCCESS: transactionId={}", transaction.getTransactionId());
        }
    }
    
    private void handleWalletDebited(Transaction transaction, WalletEventDTO event) {
        log.info("Processing wallet debited event: transactionId={}", transaction.getTransactionId());
        
        if (transaction.getStatus() == TransactionStatus.PENDING || 
            transaction.getStatus() == TransactionStatus.PROCESSING) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(Instant.now());
            transactionRepository.save(transaction);
            log.info("Transaction marked as SUCCESS: transactionId={}", transaction.getTransactionId());
        }
    }
    
    private void handleWalletHoldCreated(Transaction transaction, WalletEventDTO event) {
        log.info("Processing wallet hold created event: transactionId={}", transaction.getTransactionId());
        
        if (transaction.getStatus() == TransactionStatus.PENDING) {
            transaction.setStatus(TransactionStatus.HELD);
            transactionRepository.save(transaction);
            log.info("Transaction marked as HELD: transactionId={}", transaction.getTransactionId());
        }
    }
    
    private void handleWalletHoldReleased(Transaction transaction, WalletEventDTO event) {
        log.info("Processing wallet hold released event: transactionId={}", transaction.getTransactionId());
        
        if (transaction.getStatus() == TransactionStatus.HELD) {
            transaction.setStatus(TransactionStatus.CANCELLED);
            transaction.setCancelledAt(Instant.now());
            transaction.setCompletedAt(Instant.now());
            transactionRepository.save(transaction);
            log.info("Transaction marked as CANCELLED: transactionId={}", transaction.getTransactionId());
        }
    }
    
    private void handleInsufficientFunds(Transaction transaction, WalletEventDTO event) {
        log.info("Processing insufficient funds event: transactionId={}", transaction.getTransactionId());
        
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(event.getReason() != null ? event.getReason() : "Insufficient funds");
        transaction.setCompletedAt(Instant.now());
        transactionRepository.save(transaction);
        log.info("Transaction marked as FAILED: transactionId={}", transaction.getTransactionId());
    }
}
