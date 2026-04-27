package com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.ColaInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ColaInscripcionRepository extends JpaRepository<ColaInscripcion, Long> {

    /** ¿Está el estudiante en cola PENDIENTE para esta materia? */
    boolean existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstado(
            Long idEstudiante, Long idMateria, ColaInscripcion.Estado estado);

    /** Cola PENDIENTE de una materia ordenada FIFO (posicion ASC) */
    @Query("SELECT c FROM ColaInscripcion c WHERE c.materia.idMateria = :idMateria " +
           "AND c.estado = 'PENDIENTE' ORDER BY c.posicion ASC")
    List<ColaInscripcion> findPendientesPorMateriaOrdenados(@Param("idMateria") Long idMateria);

    /** Primera posición pendiente en la cola de una materia (para promover) */
    @Query("SELECT c FROM ColaInscripcion c WHERE c.materia.idMateria = :idMateria " +
           "AND c.estado = 'PENDIENTE' ORDER BY c.posicion ASC LIMIT 1")
    Optional<ColaInscripcion> findPrimeroPendiente(@Param("idMateria") Long idMateria);

    /** Posición máxima en la cola de una materia (para calcular la siguiente) */
    @Query("SELECT COALESCE(MAX(c.posicion), 0) FROM ColaInscripcion c " +
           "WHERE c.materia.idMateria = :idMateria AND c.estado = 'PENDIENTE'")
    int findMaxPosicion(@Param("idMateria") Long idMateria);

    /** Cola de un estudiante (sus posiciones en todas las materias) */
    @Query("SELECT c FROM ColaInscripcion c WHERE c.estudiante.idUsuario = :idEstudiante " +
           "ORDER BY c.fechaSolicitud DESC")
    List<ColaInscripcion> findByEstudianteId(@Param("idEstudiante") Long idEstudiante);

    /** Entrada específica de un estudiante en la cola de una materia */
    Optional<ColaInscripcion> findByEstudianteIdUsuarioAndMateriaIdMateria(
            Long idEstudiante, Long idMateria);

    /** Registros expirados que todavía están en PENDIENTE */
    @Query("SELECT c FROM ColaInscripcion c WHERE c.estado = 'PENDIENTE' " +
           "AND c.fechaExpiracion < :ahora")
    List<ColaInscripcion> findExpirados(@Param("ahora") LocalDateTime ahora);

    /** Cuántas posiciones hay antes del estudiante (para informar el turno) */
    @Query("SELECT COUNT(c) FROM ColaInscripcion c WHERE c.materia.idMateria = :idMateria " +
           "AND c.estado = 'PENDIENTE' AND c.posicion < :posicion")
    int countDelante(@Param("idMateria") Long idMateria, @Param("posicion") int posicion);

    /** Marcar masivamente los expirados */
    @Modifying
    @Query("UPDATE ColaInscripcion c SET c.estado = 'EXPIRADO', c.fechaResolucion = :ahora " +
           "WHERE c.estado = 'PENDIENTE' AND c.fechaExpiracion < :ahora")
    int expirarVencidos(@Param("ahora") LocalDateTime ahora);
}
