package com.example.service_portefeuilles.model;


import com.example.service_portefeuilles.dto.TransactionDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;


@Component(value = "transactionMapper")
public class TransactionMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    // Map Transaction to TransactionDTO
    public  TransactionDTO toDTO(Transaction transaction) {
        return modelMapper.map(transaction, TransactionDTO.class);
    }

    // Map TransactionDTO to Transaction
    public  Transaction toEntity(TransactionDTO transactionDTO) {
        return modelMapper.map(transactionDTO, Transaction.class);
    }
}