package com.example.cmi.repository;

import com.example.cmi.model.CompteBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompteBancaireRepository extends JpaRepository<CompteBancaire,Long> {
    CompteBancaire findByUtilisateurId(Long utilisateurId);
    Optional<CompteBancaire> findByNumeroCompte(String numeroCompte);
}
