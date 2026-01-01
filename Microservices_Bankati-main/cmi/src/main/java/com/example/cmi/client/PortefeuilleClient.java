package com.example.cmi.client;


import com.example.cmi.dto.PortefeuilleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "service-portefeuilles", url = "http://localhost:8086/api/portefeuilles")
@Component
public interface PortefeuilleClient {
    @GetMapping("/{id}")
    PortefeuilleDto getPortefeuilleById(@PathVariable("id") Long id);

    @PutMapping("/crediter/{id}/{amount}")
    String crediterPortefeuille(@PathVariable Long id, @PathVariable Double amount);
    @PutMapping("{id}/{amount}")
    String debitPortefeuille(@PathVariable Long id, @PathVariable Double amount) ;

    @PostMapping("/creer")
    public String creerPortefeuille(@RequestBody PortefeuilleDto request);

//    @PutMapping("/{id}/debit")
//    void debitPortefeuille(@PathVariable("id") Long portefeuilleId, @RequestParam("amount") Double amount);
}
