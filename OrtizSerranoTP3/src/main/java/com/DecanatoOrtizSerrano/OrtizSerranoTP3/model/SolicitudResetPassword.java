package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registra las solicitudes de "Olvidé mi contraseña".
 * El admin las consulta y asigna una nueva contraseña temporal.
 */
@Entity
@Table(name = "solicitudes_reset_password",
       indexes = {
           @Index(name = "idx_srp_email",  columnList = "email"),
           @Index(name = "idx_srp_estado", columnList = "estado")
       })
public class SolicitudResetPassword {

    public enum Estado { PENDIENTE, ATENDIDA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    /** Email del usuario que olvidó su contraseña */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** Fecha y hora en que se realizó la solicitud */
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    /** Fecha en que el admin atendió la solicitud (null si aún pendiente) */
    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    /** Nombre completo del solicitante (para que el admin lo identifique) */
    @Column(name = "nombre_completo", length = 200)
    private String nombreCompleto;

    public SolicitudResetPassword() {}

    public SolicitudResetPassword(String email, String nombreCompleto) {
        this.email = email;
        this.nombreCompleto = nombreCompleto;
        this.fechaSolicitud = LocalDateTime.now();
        this.estado = Estado.PENDIENTE;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public Long getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Long idSolicitud) { this.idSolicitud = idSolicitud; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public LocalDateTime getFechaAtencion() { return fechaAtencion; }
    public void setFechaAtencion(LocalDateTime fechaAtencion) { this.fechaAtencion = fechaAtencion; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
}
