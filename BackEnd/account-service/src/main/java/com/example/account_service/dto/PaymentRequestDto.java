package com.example.account_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    private String senderAccountRef;     // externalReference
    private String receiverAccountRef;   // externalReference
    private BigDecimal amount;
    private String motif;
}
