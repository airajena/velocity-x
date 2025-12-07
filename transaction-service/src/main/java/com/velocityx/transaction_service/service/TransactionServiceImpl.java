package com.velocityx.transaction_service.service;

import com.velocityx.transaction_service.dto.request.*;
import com.velocityx.transaction_service.dto.response.PagedResponse;
import com.velocityx.transaction_service.dto.response.TransactionResponse;
import com.velocityx.transaction_service.entity.Transaction;
import com.velocityx.transaction_service.entity.TransactionEvent;
import com.velocityx.transaction_service.enums.EventType;
import com.velocityx.transaction_service.enums.TransactionStatus;
import com.velocityx.transaction_service.enums.TransactionType;
import com.velocityx.transaction_service.exception.InvalidTransactionException;
import com.velocityx.transaction_service.exception.ResourceNotFoundException;
import com.velocityx.transaction_service.mapper.TransactionMapper;
import com.velocityx.transaction_service.repository.TransactionEventRepository;
import com.velocityx.transaction_service.repository.TransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final TransactionMapper transactionMapper;
    private final IdempotencyService idempotencyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private final Counter transactionCreatedCounter;
    private final Counter transactionSuccessCounter;
    private final Counter transactionFailedCounter;
    private final Timer transactionProcessingTimer;
    
    @Override
    @CacheEvict(value = "transactionsByUser", key = "#request.userId")
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        log.info("Creating transaction: type={}, amount={}, userId={}", 
                request.getType(), request.getAmount(), request.getUserId());
        
        return transactionProcessingTimer.record(() -> {
            if (request.getIdempotencyKey() != null) {
                TransactionResponse cached = idempotencyService.getCachedResponse(
                        request.getIdempotencyKey(), request);
                if (cached != null) {
                    log.info("Returning cached response for idempotency key: {}", 
                            request.getIdempotencyKey());
                    return cached;
                }
            }
            
            Transaction transaction = transactionMapper.toEntity(request);
            transaction.setTransactionId(generateTransactionId());
            transaction.setStatus(TransactionStatus.PENDING);
            transaction = transactionRepository.save(transaction);
            
            addEvent(transaction, EventType.TRANSACTION_CREATED, 
                    Map.of("status", "PENDING", "amount", request.getAmount()));
            
            transactionCreatedCounter.increment();
            kafkaTemplate.send("transaction-events", buildEventPayload(transaction, EventType.TRANSACTION_CREATED));
            
            TransactionResponse response = transactionMapper.toDto(transaction);
            
            if (request.getIdempotencyKey() != null) {
                idempotencyService.cacheResponse(request.getIdempotencyKey(), request, response);
            }
            
            log.info("Transaction created: id={}, transactionId={}", 
                    transaction.getId(), transaction.getTransactionId());
            
            return response;
        });
    }
    
    @Override
    @CacheEvict(value = "transactionsByUser", key = "#request.userId")
    public TransactionResponse holdFunds(HoldFundsRequest request) {
        log.info("Holding funds: amount={}, walletId={}, userId={}", 
                request.getAmount(), request.getWalletId(), request.getUserId());
        
        return transactionProcessingTimer.record(() -> {
            if (request.getIdempotencyKey() != null) {
                TransactionResponse cached = idempotencyService.getCachedResponse(
                        request.getIdempotencyKey(), request);
                if (cached != null) {
                    return cached;
                }
            }
            
            Transaction transaction = Transaction.builder()
                    .transactionId(generateTransactionId())
                    .idempotencyKey(request.getIdempotencyKey())
                    .type(TransactionType.HOLD)
                    .status(TransactionStatus.PENDING)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .userId(request.getUserId())
                    .fromWalletId(request.getWalletId())
                    .description(request.getDescription())
                    .metadata(request.getMetadata())
                    .holdExpiresAt(Instant.now().plus(request.getHoldDurationDays(), ChronoUnit.DAYS))
                    .build();
            
            transaction = transactionRepository.save(transaction);
            
            transaction.setStatus(TransactionStatus.HELD);
            transaction = transactionRepository.save(transaction);
            
            addEvent(transaction, EventType.TRANSACTION_HELD, 
                    Map.of("amount", request.getAmount(), "expiresAt", transaction.getHoldExpiresAt()));
            
            transactionCreatedCounter.increment();
            kafkaTemplate.send("transaction-events", buildEventPayload(transaction, EventType.TRANSACTION_HELD));
            
            TransactionResponse response = transactionMapper.toDto(transaction);
            
            if (request.getIdempotencyKey() != null) {
                idempotencyService.cacheResponse(request.getIdempotencyKey(), request, response);
            }
            
            log.info("Funds held: transactionId={}, expiresAt={}", 
                    transaction.getTransactionId(), transaction.getHoldExpiresAt());
            
            return response;
        });
    }
    
    @Override
    @CacheEvict(value = {"transactions", "transactionsByUser"}, allEntries = true)
    public TransactionResponse captureFunds(Long transactionId, CaptureFundsRequest request) {
        log.info("Capturing funds for transaction: {}", transactionId);
        
        return transactionProcessingTimer.record(() -> {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Transaction not found with id: " + transactionId));
            
            if (!transaction.canBeCaptured()) {
                throw new InvalidTransactionException(
                        "Transaction cannot be captured. Status: " + transaction.getStatus());
            }
            
            transaction.setStatus(TransactionStatus.CAPTURED);
            transaction.setCapturedAt(Instant.now());
            transaction.setCompletedAt(Instant.now());
            
            if (request.getDescription() != null) {
                transaction.setDescription(request.getDescription());
            }
            
            transaction = transactionRepository.save(transaction);
            
            addEvent(transaction, EventType.TRANSACTION_CAPTURED, 
                    Map.of("capturedAt", transaction.getCapturedAt()));
            
            transactionSuccessCounter.increment();
            kafkaTemplate.send("transaction-events", buildEventPayload(transaction, EventType.TRANSACTION_CAPTURED));
            
            log.info("Funds captured: transactionId={}", transaction.getTransactionId());
            
            return transactionMapper.toDto(transaction);
        });
    }
    
    @Override
    @CacheEvict(value = {"transactions", "transactionsByUser"}, allEntries = true)
    public TransactionResponse cancelTransaction(Long transactionId) {
        log.info("Cancelling transaction: {}", transactionId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + transactionId));
        
        if (!transaction.canBeCancelled()) {
            throw new InvalidTransactionException(
                    "Transaction cannot be cancelled. Status: " + transaction.getStatus());
        }
        
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setCancelledAt(Instant.now());
        transaction.setCompletedAt(Instant.now());
        transaction = transactionRepository.save(transaction);
        
        addEvent(transaction, EventType.TRANSACTION_CANCELLED, 
                Map.of("cancelledAt", transaction.getCancelledAt()));
        
        kafkaTemplate.send("transaction-events", buildEventPayload(transaction, EventType.TRANSACTION_CANCELLED));
        
        log.info("Transaction cancelled: transactionId={}", transaction.getTransactionId());
        
        return transactionMapper.toDto(transaction);
    }
    
    @Override
    @CacheEvict(value = {"transactions", "transactionsByUser"}, allEntries = true)
    public TransactionResponse refundTransaction(Long transactionId, RefundRequest request) {
        log.info("Refunding transaction: {}, amount={}", transactionId, request.getAmount());
        
        return transactionProcessingTimer.record(() -> {
            Transaction originalTransaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Transaction not found with id: " + transactionId));
            
            if (!originalTransaction.canBeRefunded()) {
                throw new InvalidTransactionException(
                        "Transaction cannot be refunded. Status: " + originalTransaction.getStatus());
            }
            
            BigDecimal refundAmount = request.getAmount() != null ? 
                    request.getAmount() : originalTransaction.getAmount();
            
            if (refundAmount.compareTo(originalTransaction.getAmount()) > 0) {
                throw new InvalidTransactionException(
                        "Refund amount cannot exceed original transaction amount");
            }
            
            Transaction refundTransaction = Transaction.builder()
                    .transactionId(generateTransactionId())
                    .idempotencyKey(request.getIdempotencyKey())
                    .type(TransactionType.REFUND)
                    .status(TransactionStatus.SUCCESS)
                    .amount(refundAmount)
                    .currency(originalTransaction.getCurrency())
                    .userId(originalTransaction.getUserId())
                    .toWalletId(originalTransaction.getFromWalletId())
                    .description(request.getReason())
                    .metadata(request.getMetadata())
                    .parentTransactionId(originalTransaction.getId())
                    .completedAt(Instant.now())
                    .build();
            
            refundTransaction = transactionRepository.save(refundTransaction);
            
            originalTransaction.setStatus(TransactionStatus.REFUNDED);
            originalTransaction.setRefundedAt(Instant.now());
            transactionRepository.save(originalTransaction);
            
            addEvent(refundTransaction, EventType.TRANSACTION_REFUNDED, 
                    Map.of("originalTransactionId", originalTransaction.getTransactionId(), 
                           "refundAmount", refundAmount));
            
            transactionSuccessCounter.increment();
            kafkaTemplate.send("transaction-events", buildEventPayload(refundTransaction, EventType.TRANSACTION_REFUNDED));
            
            log.info("Transaction refunded: originalId={}, refundId={}", 
                    originalTransaction.getTransactionId(), refundTransaction.getTransactionId());
            
            return transactionMapper.toDto(refundTransaction);
        });
    }
    
    @Override
    @CacheEvict(value = "transactionsByUser", allEntries = true)
    public TransactionResponse transferFunds(TransferRequest request) {
        log.info("Transferring funds: from={}, to={}, amount={}", 
                request.getFromWalletId(), request.getToWalletId(), request.getAmount());
        
        return transactionProcessingTimer.record(() -> {
            if (request.getIdempotencyKey() != null) {
                TransactionResponse cached = idempotencyService.getCachedResponse(
                        request.getIdempotencyKey(), request);
                if (cached != null) {
                    return cached;
                }
            }
            
            Transaction transaction = Transaction.builder()
                    .transactionId(generateTransactionId())
                    .idempotencyKey(request.getIdempotencyKey())
                    .type(TransactionType.TRANSFER)
                    .status(TransactionStatus.PENDING)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .userId(request.getUserId())
                    .fromWalletId(request.getFromWalletId())
                    .toWalletId(request.getToWalletId())
                    .description(request.getDescription())
                    .metadata(request.getMetadata())
                    .build();
            
            transaction = transactionRepository.save(transaction);
            
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(Instant.now());
            transaction = transactionRepository.save(transaction);
            
            addEvent(transaction, EventType.TRANSACTION_SUCCESS, 
                    Map.of("from", request.getFromWalletId(), 
                           "to", request.getToWalletId(), 
                           "amount", request.getAmount()));
            
            transactionSuccessCounter.increment();
            kafkaTemplate.send("transaction-events", buildEventPayload(transaction, EventType.TRANSACTION_SUCCESS));
            
            TransactionResponse response = transactionMapper.toDto(transaction);
            
            if (request.getIdempotencyKey() != null) {
                idempotencyService.cacheResponse(request.getIdempotencyKey(), request, response);
            }
            
            log.info("Funds transferred: transactionId={}", transaction.getTransactionId());
            
            return response;
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "transactions", key = "#id")
    public TransactionResponse getTransactionById(Long id) {
        log.debug("Fetching transaction by id: {}", id);
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + id));
        
        return transactionMapper.toDto(transaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "transactions", key = "#transactionId")
    public TransactionResponse getTransactionByTransactionId(String transactionId) {
        log.debug("Fetching transaction by transactionId: {}", transactionId);
        
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with transactionId: " + transactionId));
        
        return transactionMapper.toDto(transaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "transactionsByUser", key = "#userId + '_' + #pageable.pageNumber")
    public PagedResponse<TransactionResponse> getUserTransactions(Long userId, Pageable pageable) {
        log.debug("Fetching transactions for user: {}", userId);
        
        Page<Transaction> transactionPage = transactionRepository.findByUserId(userId, pageable);
        
        List<TransactionResponse> transactions = transactionMapper.toDtoList(transactionPage.getContent());
        
        return PagedResponse.<TransactionResponse>builder()
                .content(transactions)
                .page(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .last(transactionPage.isLast())
                .first(transactionPage.isFirst())
                .empty(transactionPage.isEmpty())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getAllTransactions(Pageable pageable) {
        log.debug("Fetching all transactions");
        
        Page<Transaction> transactionPage = transactionRepository.findAll(pageable);
        
        List<TransactionResponse> transactions = transactionMapper.toDtoList(transactionPage.getContent());
        
        return PagedResponse.<TransactionResponse>builder()
                .content(transactions)
                .page(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .last(transactionPage.isLast())
                .first(transactionPage.isFirst())
                .empty(transactionPage.isEmpty())
                .build();
    }
    
    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
    }
    
    private void addEvent(Transaction transaction, EventType eventType, Map<String, Object> eventData) {
        TransactionEvent event = TransactionEvent.builder()
                .transaction(transaction)
                .eventType(eventType)
                .eventData(eventData != null ? eventData : new HashMap<>())
                .build();
        
        eventRepository.save(event);
    }
    
    private Map<String, Object> buildEventPayload(Transaction transaction, EventType eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", transaction.getTransactionId());
        payload.put("eventType", eventType.name());
        payload.put("type", transaction.getType().name());
        payload.put("status", transaction.getStatus().name());
        payload.put("amount", transaction.getAmount());
        payload.put("currency", transaction.getCurrency());
        payload.put("userId", transaction.getUserId());
        payload.put("timestamp", Instant.now());
        return payload;
    }
}