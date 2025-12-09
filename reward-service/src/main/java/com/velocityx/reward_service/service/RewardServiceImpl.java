package com.velocityx.reward_service.service;

import com.velocityx.reward_service.dto.event.RewardEvent;
import com.velocityx.reward_service.dto.request.*;
import com.velocityx.reward_service.dto.response.BalanceResponse;
import com.velocityx.reward_service.dto.response.TransactionResponse;
import com.velocityx.reward_service.entity.RewardAccount;
import com.velocityx.reward_service.entity.RewardTransaction;
import com.velocityx.reward_service.enums.*;
import com.velocityx.reward_service.repository.RewardAccountRepository;
import com.velocityx.reward_service.repository.RewardTransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RewardServiceImpl implements RewardService {
    
    private final RewardAccountRepository accountRepository;
    private final RewardTransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    
    @Value("${kafka.topics.rewards-events}")
    private String rewardsEventsTopic;
    
    @Value("${reward.hold.expiry-hours:24}")
    private int holdExpiryHours;
    
    @Value("${reward.ledger.system-account-id:SYSTEM_ACCOUNT}")
    private String systemAccountId;
    
    @Value("${reward.ledger.platform-account-id:PLATFORM_RESERVE}")
    private String platformAccountId;
    
    @Override
    public TransactionResponse earn(EarnRequest request) {
        log.info("Processing earn request: userId={}, amount={}, idempotencyKey={}", 
                request.getUserId(), request.getAmount(), request.getIdempotencyKey());
        
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return getExistingTransaction(request.getIdempotencyKey());
        }
        
        RewardAccount userAccount = getOrCreateUserAccount(request.getUserId());
        
        RewardTransaction transaction = RewardTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey(request.getIdempotencyKey())
                .userId(request.getUserId())
                .accountId(userAccount.getAccountId())
                .transactionType(TransactionType.EARN)
                .status(TransactionStatus.INIT)
                .amount(request.getAmount())
                .description("Earn: " + request.getEventType())
                .metadata(request.getMetadata())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        publishEvent(RewardEventType.EARN_REQUESTED, transaction);
        
        incrementMetric("rewards.earn.requested");
        
        return toResponse(transaction);
    }
    
    @Override
    public TransactionResponse hold(HoldRequest request) {
        log.info("Processing hold request: userId={}, amount={}, idempotencyKey={}", 
                request.getUserId(), request.getAmount(), request.getIdempotencyKey());
        
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return getExistingTransaction(request.getIdempotencyKey());
        }
        
        RewardAccount userAccount = accountRepository.findByUserIdForUpdate(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Account not found for user: " + request.getUserId()));
        
        if (!userAccount.hasAvailableBalance(request.getAmount())) {
            throw new RuntimeException("Insufficient balance: available=" + userAccount.getBalanceAvailable() 
                    + ", requested=" + request.getAmount());
        }
        
        RewardTransaction transaction = RewardTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey(request.getIdempotencyKey())
                .userId(request.getUserId())
                .accountId(userAccount.getAccountId())
                .transactionType(TransactionType.HOLD)
                .status(TransactionStatus.INIT)
                .amount(request.getAmount())
                .description(request.getReason())
                .holdExpiresAt(Instant.now().plus(holdExpiryHours, ChronoUnit.HOURS))
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        publishEvent(RewardEventType.HOLD_REQUESTED, transaction);
        
        incrementMetric("rewards.hold.requested");
        
        return toResponse(transaction);
    }
    
    @Override
    public TransactionResponse capture(CaptureRequest request) {
        log.info("Processing capture request: transactionId={}, idempotencyKey={}", 
                request.getTransactionId(), request.getIdempotencyKey());
        
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return getExistingTransaction(request.getIdempotencyKey());
        }
        
        RewardTransaction holdTransaction = transactionRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Hold transaction not found: " + request.getTransactionId()));
        
        if (holdTransaction.getStatus() != TransactionStatus.HELD) {
            throw new RuntimeException("Transaction is not in HELD status: " + holdTransaction.getStatus());
        }
        
        if (!holdTransaction.isHoldActive()) {
            throw new RuntimeException("Hold has expired");
        }
        
        RewardTransaction captureTransaction = RewardTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey(request.getIdempotencyKey())
                .userId(holdTransaction.getUserId())
                .accountId(holdTransaction.getAccountId())
                .transactionType(TransactionType.CAPTURE)
                .status(TransactionStatus.INIT)
                .amount(holdTransaction.getAmount())
                .description("Capture of hold: " + holdTransaction.getTransactionId())
                .holdTransactionId(holdTransaction.getTransactionId())
                .build();
        
        captureTransaction = transactionRepository.save(captureTransaction);
        
        publishEvent(RewardEventType.CAPTURE_REQUESTED, captureTransaction);
        
        incrementMetric("rewards.capture.requested");
        
        return toResponse(captureTransaction);
    }
    
    @Override
    public TransactionResponse release(ReleaseRequest request) {
        log.info("Processing release request: transactionId={}", request.getTransactionId());
        
        RewardTransaction holdTransaction = transactionRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Hold transaction not found: " + request.getTransactionId()));
        
        if (holdTransaction.getStatus() != TransactionStatus.HELD) {
            throw new RuntimeException("Transaction is not in HELD status: " + holdTransaction.getStatus());
        }
        
        RewardTransaction releaseTransaction = RewardTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey("RELEASE-" + holdTransaction.getTransactionId())
                .userId(holdTransaction.getUserId())
                .accountId(holdTransaction.getAccountId())
                .transactionType(TransactionType.RELEASE)
                .status(TransactionStatus.INIT)
                .amount(holdTransaction.getAmount())
                .description("Release of hold: " + holdTransaction.getTransactionId())
                .holdTransactionId(holdTransaction.getTransactionId())
                .build();
        
        releaseTransaction = transactionRepository.save(releaseTransaction);
        
        publishEvent(RewardEventType.RELEASE_REQUESTED, releaseTransaction);
        
        incrementMetric("rewards.release.requested");
        
        return toResponse(releaseTransaction);
    }
    
    @Override
    public TransactionResponse adminAdjust(AdminAdjustRequest request) {
        log.info("Processing admin adjustment: userId={}, amount={}", 
                request.getUserId(), request.getAmount());
        
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return getExistingTransaction(request.getIdempotencyKey());
        }
        
        RewardAccount userAccount = getOrCreateUserAccount(request.getUserId());
        
        RewardTransaction transaction = RewardTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey(request.getIdempotencyKey())
                .userId(request.getUserId())
                .accountId(userAccount.getAccountId())
                .transactionType(TransactionType.ADJUSTMENT)
                .status(TransactionStatus.INIT)
                .amount(request.getAmount())
                .description("Admin adjustment: " + request.getReason())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        publishEvent(RewardEventType.ADJUSTMENT, transaction);
        
        incrementMetric("rewards.adjustment.requested");
        
        return toResponse(transaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId) {
        log.info("Getting balance for user: {}", userId);
        
        RewardAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Account not found for user: " + userId));
        
        List<RewardTransaction> recentTransactions = transactionRepository
                .findRecentByUserId(userId, PageRequest.of(0, 10));
        
        return BalanceResponse.builder()
                .userId(userId)
                .accountId(account.getAccountId())
                .balanceAvailable(account.getBalanceAvailable())
                .balanceReserved(account.getBalanceReserved())
                .totalBalance(account.getTotalBalance())
                .totalEarned(account.getTotalEarned())
                .totalRedeemed(account.getTotalRedeemed())
                .recentTransactions(recentTransactions.stream().map(this::toResponse).toList())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        RewardTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        return toResponse(transaction);
    }
    
    private RewardAccount getOrCreateUserAccount(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    RewardAccount newAccount = RewardAccount.builder()
                            .accountId(generateAccountId(userId))
                            .userId(userId)
                            .accountType(AccountType.USER)
                            .balanceAvailable(BigDecimal.ZERO)
                            .balanceReserved(BigDecimal.ZERO)
                            .build();
                    return accountRepository.save(newAccount);
                });
    }
    
    private TransactionResponse getExistingTransaction(String idempotencyKey) {
        RewardTransaction transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return toResponse(transaction);
    }
    
    private void publishEvent(RewardEventType eventType, RewardTransaction transaction) {
        RewardEvent event = RewardEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .transactionId(transaction.getTransactionId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .userId(transaction.getUserId())
                .accountId(transaction.getAccountId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .holdTransactionId(transaction.getHoldTransactionId())
                .metadata(transaction.getMetadata())
                .timestamp(Instant.now())
                .build();
        
        kafkaTemplate.send(rewardsEventsTopic, transaction.getTransactionId(), event);
        log.info("Published event: type={}, transactionId={}", eventType, transaction.getTransactionId());
    }
    
    private String generateTransactionId() {
        return "RWD-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
    }
    
    private String generateAccountId(Long userId) {
        return "ACC-" + userId + "-" + System.currentTimeMillis();
    }
    
    private void incrementMetric(String name) {
        Counter.builder(name).register(meterRegistry).increment();
    }
    
    private TransactionResponse toResponse(RewardTransaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .holdTransactionId(transaction.getHoldTransactionId())
                .holdExpiresAt(transaction.getHoldExpiresAt())
                .completedAt(transaction.getCompletedAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
