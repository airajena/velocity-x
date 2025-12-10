package com.velocityx.wallet_service.service;

import com.velocityx.wallet_service.dto.event.WalletEvent;
import com.velocityx.wallet_service.dto.request.*;
import com.velocityx.wallet_service.dto.response.TransactionResponse;
import com.velocityx.wallet_service.dto.response.WalletResponse;
import com.velocityx.wallet_service.entity.Wallet;
import com.velocityx.wallet_service.entity.WalletTransaction;
import com.velocityx.wallet_service.enums.*;
import com.velocityx.wallet_service.exception.InsufficientFundsException;
import com.velocityx.wallet_service.exception.WalletNotFoundException;
import com.velocityx.wallet_service.repository.WalletRepository;
import com.velocityx.wallet_service.repository.WalletTransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {
    
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    
    @Value("${kafka.topics.wallet-events}")
    private String walletEventsTopic;
    
    @Value("${wallet.hold.expiry-hours:24}")
    private int holdExpiryHours;
    
    @Override
    public WalletResponse createWallet(CreateWalletRequest request) {
        log.info("Creating wallet for userId={}", request.getUserId());
        
        if (walletRepository.existsByUserIdAndCurrency(request.getUserId(), request.getCurrency())) {
            Wallet existing = walletRepository.findByUserIdAndCurrency(request.getUserId(), request.getCurrency())
                    .orElseThrow();
            return toWalletResponse(existing);
        }
        
        Wallet wallet = Wallet.builder()
                .walletId(generateWalletId())
                .userId(request.getUserId())
                .currency(request.getCurrency())
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .heldBalance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();
        
        wallet = walletRepository.save(wallet);
        incrementMetric("wallet.created");
        
        log.info("Created wallet: walletId={}, userId={}", wallet.getWalletId(), wallet.getUserId());
        return toWalletResponse(wallet);
    }
    
    @Override
    public TransactionResponse credit(CreditRequest request) {
        log.info("Processing credit: userId={}, amount={}", request.getUserId(), request.getAmount());
        
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return getExistingTransaction(request.getIdempotencyKey());
        }
        
        Wallet wallet = getOrCreateWallet(request.getUserId(), request.getCurrency());
        
        WalletTransaction transaction = WalletTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey(request.getIdempotencyKey())
                .walletId(wallet.getWalletId())
                .userId(request.getUserId())
                .transactionType(TransactionType.CREDIT)
                .status(TransactionStatus.INIT)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .referenceId(request.getReferenceId())
                .metadata(request.getMetadata())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        publishEvent(WalletEventType.CREDIT_REQUESTED, transaction, null, null);
        incrementMetric("wallet.credit.requested");
        
        return toTransactionResponse(transaction);
    }
    
    @Override
    public TransactionResponse debit(DebitRequest request) {
        log.info("Processing debit: userId={}, amount={}", request.getUserId(), request.getAmount());
        
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return getExistingTransaction(request.getIdempotencyKey());
        }
        
        Wallet wallet = walletRepository.findByUserIdAndCurrency(request.getUserId(), request.getCurrency())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + request.getUserId()));
        
        if (!wallet.hasAvailableBalance(request.getAmount())) {
            throw new InsufficientFundsException("Insufficient funds: available=" + wallet.getAvailableBalance());
        }
        
        WalletTransaction transaction = WalletTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey(request.getIdempotencyKey())
                .walletId(wallet.getWalletId())
                .userId(request.getUserId())
                .transactionType(TransactionType.DEBIT)
                .status(TransactionStatus.INIT)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .referenceId(request.getReferenceId())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        publishEvent(WalletEventType.DEBIT_REQUESTED, transaction, null, null);
        incrementMetric("wallet.debit.requested");
        
        return toTransactionResponse(transaction);
    }
    
    @Override
    public TransactionResponse hold(HoldRequest request) {
        log.info("Processing hold: userId={}, amount={}", request.getUserId(), request.getAmount());
        
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return getExistingTransaction(request.getIdempotencyKey());
        }
        
        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(request.getUserId(), request.getCurrency())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + request.getUserId()));
        
        if (!wallet.hasAvailableBalance(request.getAmount())) {
            throw new InsufficientFundsException("Insufficient funds for hold: available=" + wallet.getAvailableBalance());
        }
        
        WalletTransaction transaction = WalletTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey(request.getIdempotencyKey())
                .walletId(wallet.getWalletId())
                .userId(request.getUserId())
                .transactionType(TransactionType.HOLD)
                .status(TransactionStatus.INIT)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getReason())
                .holdExpiresAt(Instant.now().plus(holdExpiryHours, ChronoUnit.HOURS))
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        publishEvent(WalletEventType.HOLD_REQUESTED, transaction, null, null);
        incrementMetric("wallet.hold.requested");
        
        return toTransactionResponse(transaction);
    }
    
    @Override
    public TransactionResponse capture(CaptureRequest request) {
        log.info("Processing capture: holdTxnId={}", request.getHoldTransactionId());
        
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return getExistingTransaction(request.getIdempotencyKey());
        }
        
        WalletTransaction holdTxn = transactionRepository.findByTransactionId(request.getHoldTransactionId())
                .orElseThrow(() -> new RuntimeException("Hold transaction not found"));
        
        if (holdTxn.getStatus() != TransactionStatus.HELD) {
            throw new RuntimeException("Transaction is not in HELD status");
        }
        
        if (!holdTxn.isHoldActive()) {
            throw new RuntimeException("Hold has expired");
        }
        
        WalletTransaction transaction = WalletTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey(request.getIdempotencyKey())
                .walletId(holdTxn.getWalletId())
                .userId(holdTxn.getUserId())
                .transactionType(TransactionType.CAPTURE)
                .status(TransactionStatus.INIT)
                .amount(holdTxn.getAmount())
                .currency(holdTxn.getCurrency())
                .description("Capture of hold: " + holdTxn.getTransactionId())
                .holdTransactionId(holdTxn.getTransactionId())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        publishEvent(WalletEventType.CAPTURE_REQUESTED, transaction, null, null);
        incrementMetric("wallet.capture.requested");
        
        return toTransactionResponse(transaction);
    }
    
    @Override
    public TransactionResponse release(String holdTransactionId) {
        log.info("Processing release: holdTxnId={}", holdTransactionId);
        
        WalletTransaction holdTxn = transactionRepository.findByTransactionId(holdTransactionId)
                .orElseThrow(() -> new RuntimeException("Hold transaction not found"));
        
        if (holdTxn.getStatus() != TransactionStatus.HELD) {
            throw new RuntimeException("Transaction is not in HELD status");
        }
        
        WalletTransaction transaction = WalletTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey("RELEASE-" + holdTransactionId)
                .walletId(holdTxn.getWalletId())
                .userId(holdTxn.getUserId())
                .transactionType(TransactionType.RELEASE)
                .status(TransactionStatus.INIT)
                .amount(holdTxn.getAmount())
                .currency(holdTxn.getCurrency())
                .description("Release of hold: " + holdTxn.getTransactionId())
                .holdTransactionId(holdTxn.getTransactionId())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        publishEvent(WalletEventType.RELEASE_REQUESTED, transaction, null, null);
        incrementMetric("wallet.release.requested");
        
        return toTransactionResponse(transaction);
    }
    
    @Override
    public TransactionResponse transfer(TransferRequest request) {
        log.info("Processing transfer: from={}, to={}, amount={}", 
                request.getFromUserId(), request.getToUserId(), request.getAmount());
        
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return getExistingTransaction(request.getIdempotencyKey());
        }
        
        Wallet senderWallet = walletRepository.findByUserIdAndCurrency(request.getFromUserId(), request.getCurrency())
                .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));
        
        Wallet receiverWallet = walletRepository.findByUserIdAndCurrency(request.getToUserId(), request.getCurrency())
                .orElseThrow(() -> new WalletNotFoundException("Receiver wallet not found"));
        
        if (!senderWallet.hasAvailableBalance(request.getAmount())) {
            throw new InsufficientFundsException("Insufficient funds for transfer");
        }
        
        WalletTransaction transaction = WalletTransaction.builder()
                .transactionId(generateTransactionId())
                .idempotencyKey(request.getIdempotencyKey())
                .walletId(senderWallet.getWalletId())
                .userId(request.getFromUserId())
                .transactionType(TransactionType.TRANSFER_OUT)
                .status(TransactionStatus.INIT)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .counterpartyWalletId(receiverWallet.getWalletId())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        publishEvent(WalletEventType.TRANSFER_REQUESTED, transaction, 
                receiverWallet.getWalletId(), request.getToUserId());
        incrementMetric("wallet.transfer.requested");
        
        return toTransactionResponse(transaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        return toWalletResponse(wallet);
    }
    
    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByWalletId(String walletId) {
        Wallet wallet = walletRepository.findByWalletId(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
        return toWalletResponse(wallet);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        WalletTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        return toTransactionResponse(transaction);
    }
    
    private Wallet getOrCreateWallet(Long userId, String currency) {
        return walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .walletId(generateWalletId())
                            .userId(userId)
                            .currency(currency)
                            .balance(BigDecimal.ZERO)
                            .availableBalance(BigDecimal.ZERO)
                            .heldBalance(BigDecimal.ZERO)
                            .status(WalletStatus.ACTIVE)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }
    
    private TransactionResponse getExistingTransaction(String idempotencyKey) {
        WalletTransaction transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return toTransactionResponse(transaction);
    }
    
    private void publishEvent(WalletEventType eventType, WalletTransaction transaction,
                              String counterpartyWalletId, Long counterpartyUserId) {
        WalletEvent event = WalletEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .transactionId(transaction.getTransactionId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .userId(transaction.getUserId())
                .walletId(transaction.getWalletId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .holdTransactionId(transaction.getHoldTransactionId())
                .counterpartyWalletId(counterpartyWalletId)
                .counterpartyUserId(counterpartyUserId)
                .metadata(transaction.getMetadata())
                .timestamp(Instant.now())
                .build();
        
        kafkaTemplate.send(walletEventsTopic, transaction.getUserId().toString(), event);
        log.info("Published event: type={}, txnId={}", eventType, transaction.getTransactionId());
    }
    
    private String generateWalletId() {
        return "WLT-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
    }
    
    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
    }
    
    private void incrementMetric(String name) {
        Counter.builder(name).register(meterRegistry).increment();
    }
    
    private WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .walletId(wallet.getWalletId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .availableBalance(wallet.getAvailableBalance())
                .heldBalance(wallet.getHeldBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
    
    private TransactionResponse toTransactionResponse(WalletTransaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .walletId(transaction.getWalletId())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .holdTransactionId(transaction.getHoldTransactionId())
                .holdExpiresAt(transaction.getHoldExpiresAt())
                .completedAt(transaction.getCompletedAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
