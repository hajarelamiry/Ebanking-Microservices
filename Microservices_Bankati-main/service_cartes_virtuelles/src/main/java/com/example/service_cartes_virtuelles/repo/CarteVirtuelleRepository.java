package com.example.service_cartes_virtuelles.repo;

import org.example.entites.CarteVirtuelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarteVirtuelleRepository extends JpaRepository<CarteVirtuelle, Long> {
    List<CarteVirtuelle> findByUtilisateurId(Long utilisateurId);
    Optional<CarteVirtuelle> findByCvv(String cvv);

    @Query("SELECT c.id FROM CarteVirtuelle c WHERE c.cvv = :cvv")
    Long findIdByCvv(@Param("cvv") String cvv);



}
