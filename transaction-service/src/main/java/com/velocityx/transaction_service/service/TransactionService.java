package com.velocityx.transaction_service.service;

import com.velocityx.transaction_service.dto.request.*;
import com.velocityx.transaction_service.dto.response.PagedResponse;
import com.velocityx.transaction_service.dto.response.TransactionResponse;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    
    TransactionResponse createTransaction(CreateTransactionRequest request);
    
    TransactionResponse holdFunds(HoldFundsRequest request);
    
    TransactionResponse captureFunds(Long transactionId, CaptureFundsRequest request);
    
    TransactionResponse cancelTransaction(Long transactionId);
    
    TransactionResponse refundTransaction(Long transactionId, RefundRequest request);
    
    TransactionResponse transferFunds(TransferRequest request);
    
    TransactionResponse getTransactionById(Long id);
    
    TransactionResponse getTransactionByTransactionId(String transactionId);
    
    PagedResponse<TransactionResponse> getUserTransactions(Long userId, Pageable pageable);
    
    PagedResponse<TransactionResponse> getAllTransactions(Pageable pageable);
}
