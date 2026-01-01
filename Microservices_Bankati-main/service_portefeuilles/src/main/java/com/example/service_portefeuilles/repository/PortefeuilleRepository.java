package com.example.service_portefeuilles.repository;

import com.example.service_portefeuilles.dto.PortefeuilleDto;
import com.example.service_portefeuilles.model.Portefeuille;
import org.example.enums.Devise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortefeuilleRepository extends JpaRepository<Portefeuille,Long> {
    List<Portefeuille> findByUtilisateurId(Long utilisateurId);

    Optional<Portefeuille> findByUtilisateurIdAndDevise(Long utilisateurId, Devise devise);

}
