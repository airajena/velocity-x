package com.velocityx.transaction_service.kafka.producer;

import com.velocityx.transaction_service.dto.event.TransactionEventDTO;
import com.velocityx.transaction_service.entity.Transaction;
import com.velocityx.transaction_service.enums.EventType;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Tracer tracer;
    
    @Value("${kafka.topics.transaction-events}")
    private String transactionEventsTopic;
    
    public void publishTransactionEvent(Transaction transaction, EventType eventType) {
        log.info("Publishing transaction event: transactionId={}, eventType={}", 
                transaction.getTransactionId(), eventType);
        
        TransactionEventDTO event = buildTransactionEvent(transaction, eventType);
        
        CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(transactionEventsTopic, transaction.getTransactionId(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transaction event published successfully: transactionId={}, offset={}", 
                        transaction.getTransactionId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish transaction event: transactionId={}", 
                        transaction.getTransactionId(), ex);
            }
        });
    }
    
    public void publishTransactionCreated(Transaction transaction) {
        publishTransactionEvent(transaction, EventType.TRANSACTION_CREATED);
    }
    
    public void publishTransactionSuccess(Transaction transaction) {
        publishTransactionEvent(transaction, EventType.TRANSACTION_SUCCESS);
    }
    
    public void publishTransactionFailed(Transaction transaction, String reason) {
        log.info("Publishing transaction failed event: transactionId={}, reason={}", 
                transaction.getTransactionId(), reason);
        
        TransactionEventDTO event = buildTransactionEvent(transaction, EventType.TRANSACTION_FAILED);
        Map<String, Object> metadata = new HashMap<>(event.getMetadata());
        metadata.put("failureReason", reason);
        event.setMetadata(metadata);
        
        kafkaTemplate.send(transactionEventsTopic, transaction.getTransactionId(), event);
    }
    
    public void publishTransactionHeld(Transaction transaction) {
        publishTransactionEvent(transaction, EventType.TRANSACTION_HELD);
    }
    
    public void publishTransactionCaptured(Transaction transaction) {
        publishTransactionEvent(transaction, EventType.TRANSACTION_CAPTURED);
    }
    
    public void publishTransactionCancelled(Transaction transaction) {
        publishTransactionEvent(transaction, EventType.TRANSACTION_CANCELLED);
    }
    
    public void publishTransactionRefunded(Transaction transaction) {
        publishTransactionEvent(transaction, EventType.TRANSACTION_REFUNDED);
    }
    
    private TransactionEventDTO buildTransactionEvent(Transaction transaction, EventType eventType) {
        return TransactionEventDTO.builder()
                .transactionId(transaction.getTransactionId())
                .eventType(eventType)
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .userId(transaction.getUserId())
                .fromWalletId(transaction.getFromWalletId())
                .toWalletId(transaction.getToWalletId())
                .metadata(transaction.getMetadata() != null ? transaction.getMetadata() : new HashMap<>())
                .timestamp(Instant.now())
                .correlationId(getTraceId())
                .build();
    }
    
    private String getTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return null;
    }
}
