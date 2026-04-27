package com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.SolicitudResetPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudResetPasswordRepository extends JpaRepository<SolicitudResetPassword, Long> {

    /** Obtener solicitudes por estado, ordenadas más recientes primero */
    List<SolicitudResetPassword> findByEstadoOrderByFechaSolicitudDesc(SolicitudResetPassword.Estado estado);

    /** Todas las solicitudes ordenadas más recientes primero */
    List<SolicitudResetPassword> findAllByOrderByFechaSolicitudDesc();

    /** ¿Existe solicitud pendiente para este email? (evitar duplicados) */
    boolean existsByEmailAndEstado(String email, SolicitudResetPassword.Estado estado);
}
