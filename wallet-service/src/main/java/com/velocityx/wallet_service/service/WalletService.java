package com.velocityx.wallet_service.service;

import com.velocityx.wallet_service.dto.request.*;
import com.velocityx.wallet_service.dto.response.TransactionResponse;
import com.velocityx.wallet_service.dto.response.WalletResponse;

public interface WalletService {
    
    WalletResponse createWallet(CreateWalletRequest request);
    
    TransactionResponse credit(CreditRequest request);
    
    TransactionResponse debit(DebitRequest request);
    
    TransactionResponse hold(HoldRequest request);
    
    TransactionResponse capture(CaptureRequest request);
    
    TransactionResponse release(String holdTransactionId);
    
    TransactionResponse transfer(TransferRequest request);
    
    WalletResponse getWallet(Long userId);
    
    WalletResponse getWalletByWalletId(String walletId);
    
    TransactionResponse getTransaction(String transactionId);
}
