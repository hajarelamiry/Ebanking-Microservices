package com.example.account_service.entity;
import com.example.account_service.dto.TransactionDto;
import com.example.account_service.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    // Entity -> DTO
    public TransactionDto toDto(Transaction transaction) {
        if (transaction == null) return null;

        return TransactionDto.builder()
                .transactionId(transaction.getTransactionId())
                .senderAccountRef(
                        transaction.getDestinateur().getExternalReference()
                )
                .receiverAccountRef(
                        transaction.getDestinataire().getExternalReference()
                )
                .montant(transaction.getMontant())
                .status(transaction.getStatus())
                .type(transaction.getType())
                .dateHeure(transaction.getDateHeure())
                .motif(transaction.getMotif())
                .build();
    }
}
