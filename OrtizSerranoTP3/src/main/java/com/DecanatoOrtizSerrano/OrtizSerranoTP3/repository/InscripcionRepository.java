package com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByEstudianteIdUsuario(Long idEstudiante);

    List<Inscripcion> findByMateriaIdMateria(Long idMateria);

    Optional<Inscripcion> findByEstudianteIdUsuarioAndMateriaIdMateria(Long idEstudiante, Long idMateria);

    boolean existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstadoNot(
        Long idEstudiante, Long idMateria, String estado);

    /**
     * Cuenta inscripciones activas (no CANCELADA) para una materia.
     * Usa el índice compuesto idx_inscripciones_mat_estado → SELECT COUNT(*) sin cargar entidades.
     * Es la consulta crítica para el control de cupos bajo alta concurrencia.
     */
    @Query("SELECT COUNT(i) FROM Inscripcion i WHERE i.materia.idMateria = :idMateria AND i.estado <> 'CANCELADA'")
    long countCuposOcupados(@Param("idMateria") Long idMateria);
}
