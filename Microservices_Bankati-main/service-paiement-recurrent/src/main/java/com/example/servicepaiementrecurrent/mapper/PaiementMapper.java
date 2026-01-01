package com.example.servicepaiementrecurrent.mapper;

import org.example.dto.PaiementRecurrentDTO;
import org.example.entites.PaiementRecurrent;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.Mapping;

import java.util.List;

@Component("PaiementMapper")
public class PaiementMapper {
    private static final ModelMapper modelMapper = new ModelMapper();
    // Custom mapping: Only map utilisateur.id to utilisateurId in DTO


    // Map Transaction to TransactionDTO
    public PaiementRecurrentDTO toDTO(PaiementRecurrent paiementRecurrent) {
        return modelMapper.map(paiementRecurrent, PaiementRecurrentDTO.class);
    }

    // Map TransactionDTO to Transaction
    public  PaiementRecurrent toEntity(PaiementRecurrentDTO paiementRecurrentDTO) {
        return modelMapper.map(paiementRecurrentDTO, PaiementRecurrent.class);
    }
    public List<PaiementRecurrentDTO> toDTO(List<PaiementRecurrent> paiementsRecurrents) {
        return paiementsRecurrents.stream()
                .map(this::toDTO) // Mapper chaque élément
                .toList(); // Collecter en liste
    }
}
