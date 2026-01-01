package com.example.account_service.service.Impl;

import com.example.account_service.dto.*;
import com.example.account_service.entity.*;
import com.example.account_service.enums.*;
import com.example.account_service.repository.*;
import com.example.account_service.service.AccountService;
import com.example.account_service.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final ExchangeRateService exchangeRateService;
    private static final String SYSTEM_ACCOUNT_REF = "SYSTEM-BANK-REF";

    @Override
    public AccountDto createAccount(CreateAccountRequestDto request, String userId) {
        // Correction ici : userId est maintenant un String
        boolean exists = accountRepository.existsByUtilisateurIdAndDevise(userId, request.getDevise());

        if (exists) {
            throw new RuntimeException("Un compte existe déjà pour cette devise");
        }

        Account account = Account.builder()
                .utilisateurId(userId) // String
                .devise(request.getDevise())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .status(AccountStatus.ACTIF)
                .build();

        return accountMapper.toDto(accountRepository.save(account));
    }

    @Override
    public SoldeResponseDto consulterSolde(String accountRef, String userId) {
        Account account = findOwnedAccount(accountRef, userId);
        return SoldeResponseDto.builder()
                .balance(account.getBalance())
                .devise(account.getDevise())
                .build();
    }

    @Override
    @Transactional
    public AccountDto creditAccount(String accountRef, BigDecimal amount, String userId) {
        Account userAccount = findOwnedAccount(accountRef, userId);

        // On récupère le compte système (la banque)
        Account systemAccount = accountRepository.findByExternalReference(SYSTEM_ACCOUNT_REF)
                .orElseThrow(() -> new RuntimeException("Erreur critique : Compte système introuvable"));

        userAccount.setBalance(userAccount.getBalance().add(amount));
        accountRepository.save(userAccount);

        // PLUS DE NULL : L'argent vient du SYSTEM vers l'USER
        saveTransaction(systemAccount, userAccount, amount, TransactionType.DEPOT, TransactionStatus.VALIDATED);

        return accountMapper.toDto(userAccount);
    }

    @Override
    @Transactional
    public AccountDto debitAccount(String accountRef, BigDecimal amount, String userId) {
        Account userAccount = findOwnedAccount(accountRef, userId);

        // On récupère le compte système (la banque)
        Account systemAccount = accountRepository.findByExternalReference(SYSTEM_ACCOUNT_REF)
                .orElseThrow(() -> new RuntimeException("Erreur critique : Compte système introuvable"));

        if (userAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Solde insuffisant");
        }

        userAccount.setBalance(userAccount.getBalance().subtract(amount));
        accountRepository.save(userAccount);

        // PLUS DE NULL : L'argent va de l'USER vers le SYSTEM
        saveTransaction(userAccount, systemAccount, amount, TransactionType.RETRAIT, TransactionStatus.VALIDATED);

        return accountMapper.toDto(userAccount);
    }
    @Override
    @Transactional
    public TransactionDto processPayment(PaymentRequestDto request, String userId) {
        Account sender = findOwnedAccount(request.getSenderAccountRef(), userId);
        Account receiver = accountRepository.findByExternalReference(request.getReceiverAccountRef())
                .orElseThrow(() -> new RuntimeException("Compte destinataire introuvable"));

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Solde insuffisant");
        }

        BigDecimal convertedAmount = request.getAmount();
        if (!sender.getDevise().equals(receiver.getDevise())) {
            Double rate = exchangeRateService.getExchangeRate(sender.getDevise(), receiver.getDevise());
            convertedAmount = request.getAmount().multiply(BigDecimal.valueOf(rate));
        }

        sender.setBalance(sender.getBalance().subtract(request.getAmount()));
        receiver.setBalance(receiver.getBalance().add(convertedAmount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = saveTransaction(sender, receiver, request.getAmount(), TransactionType.VIREMENT, TransactionStatus.VALIDATED);
        transaction.setMotif(request.getMotif());

        return transactionMapper.toDto(transaction);
    }

    @Override
    public AccountDto getAccountByUserId(String userId) {
        // Récupérer tous les comptes de l'utilisateur
        List<Account> userAccounts = accountRepository.findByUtilisateurId(userId);
        
        if (userAccounts.isEmpty()) {
            throw new RuntimeException("Aucun compte trouvé pour l'utilisateur: " + userId);
        }
        
        // Chercher d'abord le compte EUR (compte principal)
        Account account = userAccounts.stream()
                .filter(acc -> acc.getDevise() == Devise.EUR && acc.getStatus() == AccountStatus.ACTIF)
                .findFirst()
                .orElse(null);
        
        // Si EUR n'existe pas, prendre le premier compte actif disponible
        if (account == null) {
            account = userAccounts.stream()
                    .filter(acc -> acc.getStatus() == AccountStatus.ACTIF)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Aucun compte actif trouvé pour l'utilisateur: " + userId));
        }
        
        return accountMapper.toDto(account);
    }

    // Méthodes utilitaires privées (userId est String)
    private Account findOwnedAccount(String externalRef, String userId) {
        Account account = accountRepository.findByExternalReference(externalRef)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        if (!account.getUtilisateurId().equals(userId)) {
            throw new RuntimeException("Accès interdit : ce compte ne vous appartient pas");
        }

        if (account.getStatus() != AccountStatus.ACTIF) {
            throw new RuntimeException("Compte inactif");
        }
        return account;
    }

    private Transaction saveTransaction(Account sender, Account receiver, BigDecimal amount, TransactionType type, TransactionStatus status) {
        Transaction tx = Transaction.builder()
                .destinateur(sender)
                .destinataire(receiver)
                .montant(amount)
                .type(type)
                .status(status)
                .dateHeure(LocalDateTime.now())
                .build();
        return transactionRepository.save(tx);
    }
}