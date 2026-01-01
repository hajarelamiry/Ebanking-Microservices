package com.example.wallet_service.controller;

import com.example.wallet_service.dto.*;
import com.example.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * Créer un nouveau portefeuille (Wallet)
     * On récupère le userId et le Token du header (géré par Gateway ou Security)
     */
    @PostMapping
    public ResponseEntity<WalletResponseDto> createWallet(
            @Valid @RequestBody WalletRequestDto request,
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Id") String userId) {

        return new ResponseEntity<>(
                walletService.createWallet(request, userId, token),
                HttpStatus.CREATED
        );
    }

    /**
     * Ajouter une dépense à un portefeuille spécifique
     */
    @PostMapping("/{walletRef}/expenses")
    public ResponseEntity<ExpenseResponseDto> addExpense(
            @PathVariable String walletRef,
            @Valid @RequestBody ExpenseRequestDto request,
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(walletService.addExpense(walletRef, request, userId, token));
    }

    /**
     * Consulter l'état global du budget et la liste des dépenses
     */
    @GetMapping("/{walletRef}/summary")
    public ResponseEntity<WalletSummaryDto> getWalletSummary(
            @PathVariable String walletRef,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(walletService.getGlobalStatus(walletRef, userId));
    }
}
