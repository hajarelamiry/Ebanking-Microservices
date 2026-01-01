package com.example.service_cartes_virtuelles.mapper;

import org.example.dto.CarteVirtuelleDTO;
import org.example.dto.PortefeuillesDTO;
import org.example.entites.CarteVirtuelle;
import org.example.entites.Portefeuilles;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component("CartevirtuelleMapper")
public class CarteVirtuelleMapper {
    private static final ModelMapper modelMapper = new ModelMapper();
    // Custom mapping: Only map utilisateur.id to utilisateurId in DTO


    // Map Transaction to TransactionDTO
    public CarteVirtuelleDTO toDTO(CarteVirtuelle carteVirtuelle) {
        return modelMapper.map(carteVirtuelle, CarteVirtuelleDTO.class);
    }

    // Map TransactionDTO to Transaction
    public  CarteVirtuelle toEntity(CarteVirtuelleDTO carteVirtuelleDTO) {
        return modelMapper.map(carteVirtuelleDTO, CarteVirtuelle.class);
    }
}
