package com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Materia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Long> {
    boolean existsByCodigo(String codigo);
    Optional<Materia> findByCodigo(String codigo);

    /**
     * Carga la Materia con un bloqueo pesimista (SELECT ... FOR UPDATE).
     * Garantiza que ningún otro hilo pueda leer/modificar la misma fila
     * hasta que la transacción actual termine.
     *
     * Usado en InscripcionService.inscribir() para evitar sobrecupos
     * bajo condiciones de carrera (race conditions).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Materia m WHERE m.idMateria = :id")
    Optional<Materia> findByIdForUpdate(@Param("id") Long id);
}
