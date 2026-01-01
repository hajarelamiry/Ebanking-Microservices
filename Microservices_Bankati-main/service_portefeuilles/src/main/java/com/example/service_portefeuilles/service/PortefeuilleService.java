package com.example.service_portefeuilles.service;
import com.example.service_portefeuilles.client.UserClient;
import com.example.service_portefeuilles.dto.PortefeuilleDto;
import com.example.service_portefeuilles.client.ExpenseClient;
import com.example.service_portefeuilles.dto.*;
import com.example.service_portefeuilles.maper.PortefeuilleMapper;
import com.example.service_portefeuilles.model.Alert;
import com.example.service_portefeuilles.model.Portefeuille;
import com.example.service_portefeuilles.model.Transaction;
import com.example.service_portefeuilles.model.TransactionMapper;
import com.example.service_portefeuilles.repository.PortefeuilleRepository;
import com.example.service_portefeuilles.repository.TransactionRepository;
import lombok.AllArgsConstructor;


import org.example.dto.PortefeuillesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PortefeuilleService {

    @Autowired
    private PortefeuilleRepository portefeuilleRepository;

//    @Autowired
//    private CompteBancaireRepository compteBancaireRepository;
    @Autowired
    private PortefeuilleMapper mapper;

    @Autowired
    private UserClient userClient;
//    @Autowired
//    private AlimentationRepository alimentationRepository;
    @Autowired
    private ExpenseClient expenseClient;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionMapper transactionMapper;



    public List<PortefeuilleDto> recupererPortefeuillesEtDépensesParUtilisateur(Long utilisateurId) {
        return portefeuilleRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .map(portefeuille -> {
                    // Récupérer les dépenses via Feign
                    List<ExpenseDTO> expenses = expenseClient.getExpensesByPortefeuille(portefeuille.getId());

                    // Créer le DTO en incluant les dépenses
                    return new PortefeuilleDto(
                            portefeuille.getId(),
                            utilisateurId,
                            portefeuille.getBalance(),
                            portefeuille.getDevise(),
                            expenses
                    );
                })
                .collect(Collectors.toList());
    }

    public SoldeResponseDto consulterSolde(Long portefeuilleId) {
        Portefeuille portefeuille = portefeuilleRepository.findById(portefeuilleId)
                .orElseThrow(() -> new RuntimeException("Portefeuille non trouvé !"));
        return new SoldeResponseDto(portefeuille.getBalance(), portefeuille.getDevise());
    }


    public Alert creerPortefeuille(CreationPortefeuilleRequestDto request) {
        Optional<Portefeuille> existingPortefeuille = portefeuilleRepository.findByUtilisateurIdAndDevise(request.getutilisateurId(), request.getDevise());

        if (existingPortefeuille.isPresent()) {
            return new Alert("Un portefeuille existe déjà pour cet utilisateur et cette devise.",LocalDate.now(), false);
        }

        Portefeuille portefeuille = new Portefeuille();
        portefeuille.setUtilisateurId(request.getutilisateurId());
        portefeuille.setDevise(request.getDevise());
        portefeuille.setBalance(request.getBalanceInitiale());
        portefeuilleRepository.save(portefeuille);

        return new Alert("Portefeuille créé avec succès avec la devise " + request.getDevise(),LocalDate.now(), true);
    }


    public PortefeuilleDto getById(Long id){
        return mapper.toDTO(portefeuilleRepository.findById(id).get());
    }

    @Transactional
    public Portefeuille debitPortefeuille(Long portefeuilleId, Double amount) {
        Portefeuille portefeuille = portefeuilleRepository.findById(portefeuilleId)
                .orElseThrow(() -> new RuntimeException("Portefeuille introuvable"));

        if (portefeuille.getBalance() < amount) {
            throw new RuntimeException("Solde insuffisant pour effectuer cette opération");
        }

        portefeuille.setBalance(portefeuille.getBalance() - amount);
        return portefeuilleRepository.save(portefeuille);
    }


    @Transactional
    public Portefeuille creditPortefeuille(Long portefeuilleId, Double amount) {
        Portefeuille portefeuille = portefeuilleRepository.findById(portefeuilleId)
                .orElseThrow(() -> new RuntimeException("Portefeuille introuvable"));

        portefeuille.setBalance(portefeuille.getBalance() + amount);
        return portefeuilleRepository.save(portefeuille);
    }


    @Transactional
    public Alert alimenterDepenseExistante(Long portefeuilleId, Long depenseId, Double montantSupplementaire) {
        // Récupérer le portefeuille
        Portefeuille portefeuille = portefeuilleRepository.findById(portefeuilleId)
                .orElseThrow(() -> new RuntimeException("Portefeuille introuvable"));

        // Vérifier que le solde du portefeuille est suffisant
        if (portefeuille.getBalance() < montantSupplementaire) {
            return new Alert("Solde insuffisant", LocalDate.now(), false);
        }

        // Alimenter la dépense via Feign
        boolean updateSuccess = expenseClient.alimenterDepense(depenseId, montantSupplementaire);

        if (!updateSuccess) {
            throw new RuntimeException("Erreur lors de l'alimentation de la dépense");
        }

        // Mettre à jour le solde du portefeuille
        portefeuille.setBalance(portefeuille.getBalance() - montantSupplementaire);
        portefeuilleRepository.save(portefeuille);

        return new Alert("Dépense alimentée avec succès", LocalDate.now(), true);
    }



    @Transactional
    public TransactionDTO processPayment(Long senderWalletId, Long receiverWalletId, Double amount) {

        // 1. Récupérer les portefeuilles source et cible
        Portefeuille senderWallet = portefeuilleRepository.findById(senderWalletId)
                .orElseThrow(() -> new RuntimeException("Sender wallet not found with id: " + senderWalletId));
        System.out.println("senderWallet : "+senderWallet.getDevise());

        Portefeuille receiverWallet = portefeuilleRepository.findById(receiverWalletId)
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

        portefeuilleRepository.save(senderWallet);
        portefeuilleRepository.save(receiverWallet);

        // 6. Enregistrer la transaction
        Transaction transaction = new Transaction();
        transaction.setDestinateur(senderWallet);
        transaction.setDestinataire(receiverWallet);
        transaction.setMontant(amount);
        transaction.setStatus("COMPLETED");

        // Save the transaction in the repository
        transaction = transactionRepository.save(transaction);

        // Map the Portefeuilles and Transaction to their DTOs
        PortefeuilleDto senderWalletDTO = mapper.toDTO(senderWallet);
        PortefeuilleDto receiverWalletDTO = mapper.toDTO(receiverWallet);

        // Map the transaction to DTO
        TransactionDTO transactionDTO = transactionMapper.toDTO(transaction);
        transactionDTO.setDestinateurId(senderWalletDTO.getId());
        transactionDTO.setDestinataireId(receiverWalletDTO.getId());

        // Map the transaction to TransactionDTO
        return transactionDTO;
    }



}
