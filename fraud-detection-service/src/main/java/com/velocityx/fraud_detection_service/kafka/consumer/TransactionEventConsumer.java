package com.velocityx.fraud_detection_service.kafka.consumer;

import com.velocityx.fraud_detection_service.dto.event.TransactionEvent;
import com.velocityx.fraud_detection_service.dto.request.FraudCheckRequest;
import com.velocityx.fraud_detection_service.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {
    
    private final FraudDetectionService fraudDetectionService;
    
    @KafkaListener(topics = "${kafka.topics.transaction-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTransactionEvent(TransactionEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received transaction event: {}", event.getTransactionId());
            
            // Convert to fraud check request
            FraudCheckRequest request = FraudCheckRequest.builder()
                    .transactionId(event.getTransactionId())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .ipAddress(event.getIpAddress())
                    .deviceId(event.getDeviceId())
                    .userAgent(event.getUserAgent())
                    .country(event.getCountry())
                    .city(event.getCity())
                    .latitude(event.getLatitude())
                    .longitude(event.getLongitude())
                    .transactionType(event.getTransactionType())
                    .build();
            
            // Perform fraud check
            fraudDetectionService.performFraudCheck(request);
            
            acknowledgment.acknowledge();
            log.info("Transaction event processed successfully: {}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error processing transaction event: {}", event.getTransactionId(), e);
            // Don't acknowledge - message will be retried
        }
    }
}
