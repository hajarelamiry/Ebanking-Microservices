package com.example.service_paiement_multidevises.mapper;

import org.example.dto.PortefeuillesDTO;
import org.example.entites.Portefeuilles;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

@Component("PotefeuillesMapper")
public class PortefeuillesMapper {
    private static final ModelMapper modelMapper = new ModelMapper();
    // Custom mapping: Only map utilisateur.id to utilisateurId in DTO


    // Map Transaction to TransactionDTO
    public  PortefeuillesDTO toDTO(Portefeuilles portefeuilles) {
        return modelMapper.map(portefeuilles, PortefeuillesDTO.class);
    }

    // Map TransactionDTO to Transaction
    public  Portefeuilles toEntity(PortefeuillesDTO portefeuillesDTO) {
        return modelMapper.map(portefeuillesDTO, Portefeuilles.class);
    }
}
