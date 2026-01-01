package com.example.cmi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.cmi.model.Alimentation;

@Repository
public interface AlimentationRepository extends JpaRepository<Alimentation,Long> {
}
