package com.velocityx.transaction_service.service;

import com.velocityx.transaction_service.dto.response.TransactionResponse;

public interface IdempotencyService {
    
    <T> TransactionResponse getCachedResponse(String idempotencyKey, T request);
    
    <T> void cacheResponse(String idempotencyKey, T request, TransactionResponse response);
    
    void cleanupExpiredKeys();
}
