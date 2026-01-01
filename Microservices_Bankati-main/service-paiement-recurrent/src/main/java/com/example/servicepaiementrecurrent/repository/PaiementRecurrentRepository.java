package com.example.servicepaiementrecurrent.repository;

import org.example.entites.PaiementRecurrent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaiementRecurrentRepository extends JpaRepository<PaiementRecurrent , Long> {
    @Query("SELECT p FROM PaiementRecurrent p WHERE p.utilisateur.id = :userId")
    List<PaiementRecurrent> findPaiementsByUserId(@Param("userId") Long userId);
}
