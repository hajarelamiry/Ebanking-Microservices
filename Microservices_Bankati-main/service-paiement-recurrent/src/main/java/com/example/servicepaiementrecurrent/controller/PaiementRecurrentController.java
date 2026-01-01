package com.example.servicepaiementrecurrent.controller;

import com.example.servicepaiementrecurrent.service.PaiementRecurrentService;
import org.example.dto.PaiementRecurrentDTO;
import org.example.entites.PaiementRecurrent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paiements-recurrents")
public class PaiementRecurrentController {
    @Autowired
    private PaiementRecurrentService service;

    @PostMapping
    public ResponseEntity<String> createRecurringPayment(@RequestBody PaiementRecurrentDTO dto) {
        service.createRecurringPayment(dto); // Création du paiement récurrent
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Paiement a été créé avec succès");
    }


    @GetMapping("/user/{userId}")
    public List<PaiementRecurrentDTO> getPaiementsByUserId(@PathVariable Long userId) {
        return service.getPaiementsByUserId(userId);
    }
}
