package com.velocityx.reward_service.service;

import com.velocityx.reward_service.dto.request.*;
import com.velocityx.reward_service.dto.response.BalanceResponse;
import com.velocityx.reward_service.dto.response.TransactionResponse;

public interface RewardService {
    
    TransactionResponse earn(EarnRequest request);
    
    TransactionResponse hold(HoldRequest request);
    
    TransactionResponse capture(CaptureRequest request);
    
    TransactionResponse release(ReleaseRequest request);
    
    TransactionResponse adminAdjust(AdminAdjustRequest request);
    
    BalanceResponse getBalance(Long userId);
    
    TransactionResponse getTransaction(String transactionId);
}
