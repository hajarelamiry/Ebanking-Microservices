package com.example.account_service.service.Impl;
import com.example.account_service.dto.*;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.Transaction;
import com.example.account_service.enums.*;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.TransactionRepository;
import com.example.account_service.service.AccountService;
import com.example.account_service.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.account_service.dto.*;
import com.example.account_service.enums.AccountStatus;
import com.example.account_service.enums.TransactionStatus;
import com.example.account_service.enums.TransactionType;
import com.example.account_service.entity.AccountMapper;
import com.example.account_service.entity.TransactionMapper;
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

    // CRÉER UN COMPTE
    public AccountDto createAccount(CreateAccountRequestDto request, Long userId) {

        boolean exists = accountRepository
                .existsByUtilisateurIdAndDevise(userId, request.getDevise());

        if (exists) {
            throw new RuntimeException("Un compte existe déjà pour cette devise");
        }

        Account account = Account.builder()
                .utilisateurId(userId)
                .devise(request.getDevise())
                .balance(
                        request.getInitialBalance() != null
                                ? request.getInitialBalance()
                                : BigDecimal.ZERO
                )
                .status(AccountStatus.ACTIF)
                .build();

        account = accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    // CONSULTER SOLDE
    public SoldeResponseDto consulterSolde(String accountRef, Long userId) {

        Account account = findOwnedAccount(accountRef, userId);

        return SoldeResponseDto.builder()
                .balance(account.getBalance())
                .devise(account.getDevise())
                .build();
    }

    // CRÉDITER UN COMPTE
    @Transactional
    public AccountDto creditAccount(String accountRef, BigDecimal amount, Long userId) {

        Account account = findOwnedAccount(accountRef, userId);

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        saveTransaction(null, account, amount, TransactionType.DEPOT, TransactionStatus.VALIDATED);

        return accountMapper.toDto(account);
    }

    // DÉBITER UN COMPTE
    @Transactional
    public AccountDto debitAccount(String accountRef, BigDecimal amount, Long userId) {

        Account account = findOwnedAccount(accountRef, userId);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Solde insuffisant");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        saveTransaction(account, null, amount, TransactionType.RETRAIT, TransactionStatus.VALIDATED);

        return accountMapper.toDto(account);
    }

    // VIREMENT / PAIEMENT
    @Transactional
    public TransactionDto processPayment(PaymentRequestDto request, Long userId) {

        Account sender = findOwnedAccount(request.getSenderAccountRef(), userId);
        Account receiver = accountRepository.findByExternalReference(
                request.getReceiverAccountRef()
        ).orElseThrow(() -> new RuntimeException("Compte destinataire introuvable"));

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Solde insuffisant");
        }

        // Conversion devise si nécessaire
        BigDecimal convertedAmount = request.getAmount();
        if (!sender.getDevise().equals(receiver.getDevise())) {

            Double rate = exchangeRateService.getExchangeRate(
                    sender.getDevise(),
                    receiver.getDevise()
            );
            convertedAmount = request.getAmount().multiply(BigDecimal.valueOf(rate));
        }

        // Débit / Crédit
        sender.setBalance(sender.getBalance().subtract(request.getAmount()));
        receiver.setBalance(receiver.getBalance().add(convertedAmount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Transaction
        Transaction transaction = saveTransaction(
                sender,
                receiver,
                request.getAmount(),
                TransactionType.VIREMENT,
                TransactionStatus.VALIDATED
        );

        transaction.setMotif(request.getMotif());

        return transactionMapper.toDto(transaction);
    }

    // =========================
    // MÉTHODES UTILITAIRES
    // =========================

    private Account findOwnedAccount(String externalRef, Long userId) {
        Account account = accountRepository.findByExternalReference(externalRef)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        if (!account.getUtilisateurId().equals(userId)) {
            throw new RuntimeException("Accès interdit");
        }

        if (account.getStatus() != AccountStatus.ACTIF) {
            throw new RuntimeException("Compte non actif");
        }

        return account;
    }

    private Transaction saveTransaction(
            Account sender,
            Account receiver,
            BigDecimal amount,
            TransactionType type,
            TransactionStatus status
    ) {
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
