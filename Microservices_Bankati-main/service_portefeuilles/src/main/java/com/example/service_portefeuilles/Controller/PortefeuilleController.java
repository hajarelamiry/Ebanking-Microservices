package com.example.service_portefeuilles.Controller;

import com.example.service_portefeuilles.dto.*;
import com.example.service_portefeuilles.model.Alert;
import com.example.service_portefeuilles.service.PortefeuilleService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portefeuilles")
@AllArgsConstructor
public class PortefeuilleController {

    @Autowired
    private final PortefeuilleService portefeuilleService;



    //cette methode permet la recuperation des portefeuille d'un utilisateur avec leurs depenses
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<PortefeuilleDto>> recupererPortefeuilles(@PathVariable Long utilisateurId) {
        List<PortefeuilleDto> portefeuilles = portefeuilleService.recupererPortefeuillesEtDépensesParUtilisateur(utilisateurId);
        return ResponseEntity.ok(portefeuilles);
    }


    //recuperation de solde de portefeuille
    @GetMapping("/solde/{portefeuilleId}")
    public ResponseEntity<SoldeResponseDto> consulterSolde(@PathVariable Long portefeuilleId) {
        SoldeResponseDto solde = portefeuilleService.consulterSolde(portefeuilleId);
        return ResponseEntity.ok(solde);
    }

    //creation d'un nouveau portefeuille
    @PostMapping("/creer")
    public ResponseEntity<Alert> creerPortefeuille(@RequestBody CreationPortefeuilleRequestDto request) {
        Alert result=portefeuilleService.creerPortefeuille(request);
        System.out.println("--------------------------------");
        System.out.println(result);
        System.out.println("--------------------------------");
        return ResponseEntity.ok(result);
    }



    //recuperation par id
    @GetMapping("/{id}")
    public ResponseEntity<PortefeuilleDto> getPortefeuilleById(@PathVariable Long id) {
        PortefeuilleDto portefeuille = portefeuilleService.getById(id);
        return ResponseEntity.ok(portefeuille);
    }


    //debiter un portefeuille c'est utilise par le expense service a la creation d'une depense depuis un portefeuille
    @PutMapping("{id}/{amount}")
    public ResponseEntity<String> debitPortefeuille(
            @PathVariable Long id,
            @PathVariable Double amount) {
        try {
            portefeuilleService.debitPortefeuille(id, amount);
            return ResponseEntity.ok("Montant débité avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PutMapping("/crediter/{id}/{amount}")
    public ResponseEntity<String> crediterPortefeuille(
            @PathVariable Long id,
            @PathVariable Double amount) {
        try {
            portefeuilleService.creditPortefeuille(id, amount);
            return ResponseEntity.ok("Montant crédité avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PutMapping("/depense/alimenter")
    public Alert alimenterDepense(@RequestBody AlimentationExpenseRequest request) {
        return portefeuilleService.alimenterDepenseExistante(request.getPortefeuilleId(), request.getExpenseId(), request.getMontant());
    }

    @PostMapping("/payment/{senderWalletId}/{receiverWalletId}/{amount}")
    public TransactionDTO makePayment(@PathVariable Long senderWalletId,
                                      @PathVariable Long receiverWalletId,
                                      @PathVariable Double amount) {
        return portefeuilleService.processPayment(senderWalletId, receiverWalletId, amount);
    }
}

