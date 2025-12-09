package com.velocityx.reward_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocityx.reward_service.dto.event.RewardEvent;
import com.velocityx.reward_service.entity.RewardAccount;
import com.velocityx.reward_service.entity.RewardTransaction;
import com.velocityx.reward_service.enums.AccountType;
import com.velocityx.reward_service.enums.RewardEventType;
import com.velocityx.reward_service.enums.TransactionStatus;
import com.velocityx.reward_service.repository.RewardAccountRepository;
import com.velocityx.reward_service.repository.RewardTransactionRepository;
import com.velocityx.reward_service.service.LedgerService;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class RewardEventConsumer {
    
    private final RewardTransactionRepository transactionRepository;
    private final RewardAccountRepository accountRepository;
    private final LedgerService ledgerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    @Value("${kafka.topics.rewards-dlq}")
    private String dlqTopic;
    
    @Value("${reward.ledger.system-account-id:SYSTEM_ACCOUNT}")
    private String systemAccountId;
    
    @Value("${reward.ledger.platform-account-id:PLATFORM_RESERVE}")
    private String platformAccountId;
    
    @KafkaListener(
            topics = {"${kafka.topics.rewards-events}"},
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeRewardEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            RewardEvent event = objectMapper.convertValue(payload, RewardEvent.class);
            log.info("Consuming reward event: type={}, transactionId={}", event.getEventType(), event.getTransactionId());
            
            processEvent(event);
            
            acknowledgment.acknowledge();
            log.info("Event processed successfully: {}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error processing reward event: key={}", key, e);
            sendToDlq(payload, e.getMessage());
            acknowledgment.acknowledge();
        }
    }
    
    private void processEvent(RewardEvent event) {
        switch (event.getEventType()) {
            case EARN_REQUESTED -> processEarn(event);
            case HOLD_REQUESTED -> processHold(event);
            case CAPTURE_REQUESTED -> processCapture(event);
            case RELEASE_REQUESTED -> processRelease(event);
            case ADJUSTMENT -> processAdjustment(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }
    
    private void processEarn(RewardEvent event) {
        RewardTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            RewardAccount userAccount = accountRepository.findByUserIdForUpdate(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            
            RewardAccount systemAccount = getOrCreateSystemAccount();
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            ledgerService.createEarnEntries(transaction, userAccount, systemAccount);
            
            transaction.complete();
            transactionRepository.save(transaction);
            
            incrementMetric("rewards.earn.completed");
            log.info("Earn completed: transactionId={}, amount={}", transaction.getTransactionId(), transaction.getAmount());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("rewards.earn.failed");
            throw e;
        }
    }
    
    private void processHold(RewardEvent event) {
        RewardTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            RewardAccount userAccount = accountRepository.findByUserIdForUpdate(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            
            if (!userAccount.hasAvailableBalance(event.getAmount())) {
                throw new RuntimeException("Insufficient balance");
            }
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            ledgerService.createHoldEntries(transaction, userAccount);
            
            transaction.setStatus(TransactionStatus.HELD);
            transactionRepository.save(transaction);
            
            incrementMetric("rewards.hold.completed");
            log.info("Hold completed: transactionId={}, amount={}", transaction.getTransactionId(), transaction.getAmount());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("rewards.hold.failed");
            throw e;
        }
    }
    
    private void processCapture(RewardEvent event) {
        RewardTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            RewardTransaction holdTransaction = transactionRepository.findByTransactionId(event.getHoldTransactionId())
                    .orElseThrow(() -> new RuntimeException("Hold transaction not found"));
            
            RewardAccount userAccount = accountRepository.findByUserIdForUpdate(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            
            RewardAccount platformAccount = getOrCreatePlatformAccount();
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            ledgerService.createCaptureEntries(transaction, userAccount, platformAccount);
            
            transaction.complete();
            holdTransaction.setStatus(TransactionStatus.CAPTURED);
            
            transactionRepository.save(transaction);
            transactionRepository.save(holdTransaction);
            
            incrementMetric("rewards.capture.completed");
            log.info("Capture completed: transactionId={}, holdId={}", transaction.getTransactionId(), holdTransaction.getTransactionId());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("rewards.capture.failed");
            throw e;
        }
    }
    
    private void processRelease(RewardEvent event) {
        RewardTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            RewardTransaction holdTransaction = transactionRepository.findByTransactionId(event.getHoldTransactionId())
                    .orElseThrow(() -> new RuntimeException("Hold transaction not found"));
            
            RewardAccount userAccount = accountRepository.findByUserIdForUpdate(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            ledgerService.createReleaseEntries(transaction, userAccount);
            
            transaction.complete();
            holdTransaction.setStatus(TransactionStatus.RELEASED);
            
            transactionRepository.save(transaction);
            transactionRepository.save(holdTransaction);
            
            incrementMetric("rewards.release.completed");
            log.info("Release completed: transactionId={}, holdId={}", transaction.getTransactionId(), holdTransaction.getTransactionId());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("rewards.release.failed");
            throw e;
        }
    }
    
    private void processAdjustment(RewardEvent event) {
        RewardTransaction transaction = getTransaction(event.getTransactionId());
        
        if (transaction.getStatus() != TransactionStatus.INIT) {
            log.warn("Transaction already processed: {}", transaction.getTransactionId());
            return;
        }
        
        try {
            RewardAccount userAccount = accountRepository.findByUserIdForUpdate(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            
            RewardAccount systemAccount = getOrCreateSystemAccount();
            
            transaction.setStatus(TransactionStatus.PENDING);
            transactionRepository.save(transaction);
            
            if (event.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                ledgerService.createEarnEntries(transaction, userAccount, systemAccount);
            } else {
                userAccount.debitAvailable(event.getAmount().abs());
                accountRepository.save(userAccount);
            }
            
            transaction.complete();
            transactionRepository.save(transaction);
            
            incrementMetric("rewards.adjustment.completed");
            log.info("Adjustment completed: transactionId={}, amount={}", transaction.getTransactionId(), transaction.getAmount());
            
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            incrementMetric("rewards.adjustment.failed");
            throw e;
        }
    }
    
    private RewardTransaction getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }
    
    private RewardAccount getOrCreateSystemAccount() {
        return accountRepository.findByAccountId(systemAccountId)
                .orElseGet(() -> {
                    RewardAccount account = RewardAccount.builder()
                            .accountId(systemAccountId)
                            .accountType(AccountType.SYSTEM)
                            .balanceAvailable(new BigDecimal("1000000000"))
                            .build();
                    return accountRepository.save(account);
                });
    }
    
    private RewardAccount getOrCreatePlatformAccount() {
        return accountRepository.findByAccountId(platformAccountId)
                .orElseGet(() -> {
                    RewardAccount account = RewardAccount.builder()
                            .accountId(platformAccountId)
                            .accountType(AccountType.PLATFORM)
                            .balanceAvailable(BigDecimal.ZERO)
                            .build();
                    return accountRepository.save(account);
                });
    }
    
    private void sendToDlq(Map<String, Object> payload, String errorMessage) {
        payload.put("errorMessage", errorMessage);
        kafkaTemplate.send(dlqTopic, payload);
        log.warn("Sent to DLQ: {}", errorMessage);
    }
    
    private void incrementMetric(String name) {
        Counter.builder(name).register(meterRegistry).increment();
    }
}
