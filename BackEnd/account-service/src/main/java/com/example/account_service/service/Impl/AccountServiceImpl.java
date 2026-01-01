package com.example.account_service.service.Impl;

import com.example.account_service.dto.*;
import com.example.account_service.entity.*;
import com.example.account_service.enums.*;
import com.example.account_service.repository.*;
import com.example.account_service.service.AccountService;
import com.example.account_service.service.ExchangeRateService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Override
    @Transactional(readOnly = true)
    public StatementResponseDto generateStatement(
            String accountRef,
            LocalDate startDate,
            LocalDate endDate,
            String userId) {

        Account account = findOwnedAccount(accountRef, userId);

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atTime(23, 59, 59);

        var transactions = transactionRepository
                .findByDestinateurOrDestinataireAndDateHeureBetween(
                        account, account, from, to
                );

        return StatementResponseDto.builder()
                .statementNumber(generateStatementNumber())
                .accountRef(accountRef)
                .startDate(startDate)
                .endDate(endDate)
                .transactions(
                        transactions.stream()
                                .map(transactionMapper::toDto)
                                .toList()
                )
                .totalTransactions(transactions.size())
                .build();
    }

    /*EXPORT*/

    @Override
    public byte[] exportStatementCsv(String accountRef, LocalDate start, LocalDate end, String userId) {

        StatementResponseDto statement = generateStatement(accountRef, start, end, userId);

        StringBuilder csv = new StringBuilder("DATE,TYPE,MONTANT,STATUS,MOTIF\n");

        statement.getTransactions().forEach(tx ->
                csv.append(tx.getDateHeure()).append(",")
                        .append(tx.getType()).append(",")
                        .append(tx.getMontant()).append(",")
                        .append(tx.getStatus()).append(",")
                        .append(tx.getMotif()).append("\n")
        );

        return csv.toString().getBytes();
    }

    @Override
    public byte[] exportStatementPdf(String accountRef,
                                     LocalDate start,
                                     LocalDate end,
                                     String userId) {

        StatementResponseDto statement =
                generateStatement(accountRef, start, end, userId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10);

            // ===== Titre =====
            Paragraph title = new Paragraph("RELEVÉ BANCAIRE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            // ===== Infos compte =====
            document.add(new Paragraph("Compte : " + accountRef, normalFont));
            document.add(new Paragraph("Numéro relevé : " + statement.getStatementNumber(), normalFont));
            document.add(new Paragraph(
                    "Période : " + start + " → " + end,
                    normalFont
            ));

            document.add(new Paragraph(" "));

            // ===== Tableau transactions =====
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            addHeader(table, "Date");
            addHeader(table, "Type");
            addHeader(table, "Montant");
            addHeader(table, "Statut");
            addHeader(table, "Motif");

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (TransactionDto tx : statement.getTransactions()) {
                table.addCell(tx.getDateHeure().format(formatter));
                table.addCell(tx.getType().name());
                table.addCell(tx.getMontant().toString());
                table.addCell(tx.getStatus().name());
                table.addCell(tx.getMotif() != null ? tx.getMotif() : "-");
            }

            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Total transactions : " + statement.getTotalTransactions(),
                    normalFont
            ));

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    /*Utilitaire*/
    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setGrayFill(0.9f);
        table.addCell(cell);
    }


    /* UTILS*/

    private String generateStatementNumber() {
        return "RELEVE-" + LocalDate.now().getYear() + "-" + System.nanoTime();
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





}