package com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Auditorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditorioRepository extends JpaRepository<Auditorio, Long> {
    boolean existsByNombre(String nombre);
    Optional<Auditorio> findByNombre(String nombre);
    List<Auditorio> findByActivoTrue();
    List<Auditorio> findByCapacidadGreaterThanEqual(Integer capacidadMinima);
}
