package com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.ReservaAuditorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaAuditorioRepository extends JpaRepository<ReservaAuditorio, Long> {

    List<ReservaAuditorio> findByAuditorioIdAuditorio(Long idAuditorio);

    List<ReservaAuditorio> findBySolicitanteIdUsuario(Long idUsuario);

    List<ReservaAuditorio> findByEstado(String estado);

    List<ReservaAuditorio> findByFecha(LocalDate fecha);

    List<ReservaAuditorio> findByFechaBetween(LocalDate desde, LocalDate hasta);

    /** Verificar si existe solapamiento de horarios para un auditorio en una fecha */
    @Query("""
        SELECT COUNT(r) > 0 FROM ReservaAuditorio r
        WHERE r.auditorio.idAuditorio = :idAuditorio
          AND r.fecha = :fecha
          AND r.estado NOT IN ('CANCELADA', 'RECHAZADA')
          AND r.horaInicio < :horaFin
          AND r.horaFin > :horaInicio
        """)
    boolean existeSolapamiento(@Param("idAuditorio") Long idAuditorio,
                               @Param("fecha") LocalDate fecha,
                               @Param("horaInicio") java.time.LocalTime horaInicio,
                               @Param("horaFin") java.time.LocalTime horaFin);
}
