package com.example.account_service.dto;

import com.example.account_service.enums.TransactionStatus;
import com.example.account_service.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {

    private String transactionId;

    private String senderAccountRef;
    private String receiverAccountRef;

    private BigDecimal montant;

    private TransactionStatus status;
    private TransactionType type;

    private LocalDateTime dateHeure;

    private String motif;
}
