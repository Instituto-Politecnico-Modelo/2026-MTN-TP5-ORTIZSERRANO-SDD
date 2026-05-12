package com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    boolean existsByLegajo(String legajo);

    /** Devuelve el mayor número usado en legajos con formato LEG-NNNNN, o null si no hay ninguno. */
    @Query("SELECT MAX(CAST(SUBSTRING(e.legajo, 5) AS integer)) FROM Estudiante e WHERE e.legajo LIKE 'LEG-%'")
    Integer findMaxLegajoNumero();
}
