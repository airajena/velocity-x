package com.velocityx.transaction_service.mapper;

import com.velocityx.transaction_service.dto.request.CreateTransactionRequest;
import com.velocityx.transaction_service.dto.response.TransactionResponse;
import com.velocityx.transaction_service.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentGatewayRef", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "parentTransactionId", ignore = true)
    @Mapping(target = "holdExpiresAt", ignore = true)
    @Mapping(target = "capturedAt", ignore = true)
    @Mapping(target = "refundedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "processingStartedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "events", ignore = true)
    Transaction toEntity(CreateTransactionRequest request);
    
    TransactionResponse toDto(Transaction transaction);
    
    List<TransactionResponse> toDtoList(List<Transaction> transactions);
}
