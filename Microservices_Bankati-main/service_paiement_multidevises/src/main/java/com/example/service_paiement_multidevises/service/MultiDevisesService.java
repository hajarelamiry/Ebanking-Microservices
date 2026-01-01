package com.example.service_paiement_multidevises.service;

import com.example.service_paiement_multidevises.mapper.CarteVirtuelleMapper;
import com.example.service_paiement_multidevises.mapper.PortefeuillesMapper;
import com.example.service_paiement_multidevises.mapper.TransactionMapper;
import com.example.service_paiement_multidevises.repository.PortefeuillesRepository;
import com.example.service_paiement_multidevises.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.example.dto.PortefeuillesDTO;
import org.example.entites.Portefeuilles;
import org.example.entites.Transaction;
import org.example.dto.TransactionDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MultiDevisesService {
    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private PortefeuillesRepository portefeuillesRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PortefeuillesMapper portefeuillesMapper;

    @Autowired
    private TransactionMapper transactionMapper;


    @Autowired
    private CarteVirtuelleMapper carteVirtuelleMapper;
    @Transactional
    public TransactionDTO processPayment(Long senderWalletId, Long receiverWalletId, Double amount) {

        // 1. Récupérer les portefeuilles source et cible
        Portefeuilles senderWallet = portefeuillesRepository.findById(senderWalletId)
                .orElseThrow(() -> new RuntimeException("Sender wallet not found with id: " + senderWalletId));
        System.out.println("senderWallet : "+senderWallet.getDevise());

        Portefeuilles receiverWallet = portefeuillesRepository.findById(receiverWalletId)
                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

        System.out.println("receiverWallet : "+receiverWallet.getDevise());

        // 2. Vérifier les fonds disponibles
        if (senderWallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient funds");
        }

        // 3. Obtenir le taux de change
        Double exchangeRate = exchangeRateService.getExchangeRate(
                senderWallet.getDevise(),
                receiverWallet.getDevise()
        );

        // 4. Calculer le montant converti
        Double convertedAmount = amount * exchangeRate;

        // 5. Débit et crédit des portefeuilles
        senderWallet.setBalance(senderWallet.getBalance() - amount);
        receiverWallet.setBalance(receiverWallet.getBalance() + convertedAmount);

        portefeuillesRepository.save(senderWallet);
        portefeuillesRepository.save(receiverWallet);

        // 6. Enregistrer la transaction
        Transaction transaction = new Transaction();
        transaction.setDestinateur(senderWallet);
        transaction.setDestinataire(receiverWallet);
        transaction.setMontant(amount);
        transaction.setStatus("COMPLETED");

        // Save the transaction in the repository
        transaction = transactionRepository.save(transaction);

        // Map the Portefeuilles and Transaction to their DTOs
        PortefeuillesDTO senderWalletDTO = portefeuillesMapper.toDTO(senderWallet);
        PortefeuillesDTO receiverWalletDTO = portefeuillesMapper.toDTO(receiverWallet);

        // Map the transaction to DTO
        TransactionDTO transactionDTO = transactionMapper.toDTO(transaction);
        transactionDTO.setDestinateurId(senderWalletDTO.getId());
        transactionDTO.setDestinataireId(receiverWalletDTO.getId());

        // Map the transaction to TransactionDTO
        return transactionDTO;
    }

    public Double getMontant(Long portefeuillesId){
        return portefeuillesRepository.findBalanceById(portefeuillesId);
    }


    public PortefeuillesDTO getPortefeuilles(Long portefeuillesId){
        return portefeuillesMapper.toDTO(portefeuillesRepository.findById(portefeuillesId).orElseThrow(() -> new RuntimeException("Portefeuille not found")));
    }



}
