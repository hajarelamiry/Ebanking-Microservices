package com.example.service_cartes_virtuelles.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "service-paiement-multidevises", url = "http://localhost:8083/api")

public interface PaiementClient {
    @PostMapping("/virtual-card/{cvv}")
     ResponseEntity<String> payWithVirtualCard(
            @PathVariable String cvv,
            @RequestParam String toCurrency,
            @RequestParam Double amount);


}

