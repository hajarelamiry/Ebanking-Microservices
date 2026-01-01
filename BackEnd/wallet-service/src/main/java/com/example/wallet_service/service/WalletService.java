package com.example.wallet_service.service;

import com.example.wallet_service.dto.*;

public interface WalletService {

    WalletResponseDto createWallet(WalletRequestDto request, String userId, String token);
    ExpenseResponseDto addExpense(String walletRef, ExpenseRequestDto request, String userId, String token);
    WalletSummaryDto getGlobalStatus(String walletRef, String userId);
}
