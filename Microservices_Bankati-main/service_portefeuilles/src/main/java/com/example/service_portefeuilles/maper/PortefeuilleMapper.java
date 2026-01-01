package com.example.service_portefeuilles.maper;

import com.example.service_portefeuilles.dto.PortefeuilleDto;
import com.example.service_portefeuilles.model.Portefeuille;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@NoArgsConstructor
public class PortefeuilleMapper {
    @Autowired
    private ModelMapper mapper;

    public PortefeuilleDto toDTO(Portefeuille portefeuilles) {
        return mapper.map(portefeuilles, PortefeuilleDto.class);
    }

    // Convertir un ExpenseDTO en entité Expense
    public Portefeuille toEntity(PortefeuilleDto portefeuilleDto) {
        return mapper.map(portefeuilleDto, Portefeuille.class);
    }
    public List<PortefeuilleDto> toDTO(List<Portefeuille> portefeuillesList) {
        return portefeuillesList.stream()
                .map(portefeuilles -> mapper.map(portefeuilles, PortefeuilleDto.class))
                .collect(Collectors.toList());
    }
}
