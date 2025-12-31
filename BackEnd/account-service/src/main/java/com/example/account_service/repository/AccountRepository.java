package com.example.account_service.repository;

import com.example.account_service.entity.Account;
import com.example.account_service.enums.Devise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // 1. Pour récupérer tous les comptes d'un utilisateur
    // On utilise "UtilisateurId" car c'est le nom exact du champ dans ton entité
    List<Account> findByUtilisateurId(String utilisateurId);

    // 2. Pour vérifier si un compte existe déjà dans cette devise (pour la création)
    boolean existsByUtilisateurIdAndDevise(String utilisateurId, Devise devise);

    // 3. LA MÉTHODE QUE TON SERVICE APPELLE :
    // Spring va automatiquement mapper "ExternalReference" au champ private String externalReference
    Optional<Account> findByExternalReference(String externalReference);

}