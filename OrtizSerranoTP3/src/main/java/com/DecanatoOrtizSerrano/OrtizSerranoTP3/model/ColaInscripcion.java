package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa la posición de un estudiante en la cola virtual de una materia.
 *
 * Cuando una materia está llena, el estudiante puede unirse a la cola.
 * Al liberarse un cupo (cancelación), el primer PENDIENTE de la cola
 * es promovido automáticamente a inscripción activa (FIFO).
 *
 * Estados:
 *  PENDIENTE  → en la cola, esperando cupo
 *  PROMOVIDO  → recibió un cupo y fue inscripto
 *  EXPIRADO   → la cola venció sin recibir cupo
 *  CANCELADO  → el estudiante abandonó la cola voluntariamente
 */
@Entity
@Table(
    name = "cola_inscripciones",
    indexes = {
        // Para consultar la cola de una materia ordenada por posición
        @Index(name = "idx_cola_materia_estado_pos",
               columnList = "id_materia, estado, posicion"),
        // Para verificar si un estudiante ya está en la cola de una materia
        @Index(name = "idx_cola_est_mat",
               columnList = "id_estudiante, id_materia"),
        // Limpieza de registros expirados
        @Index(name = "idx_cola_expiracion",
               columnList = "fecha_expiracion, estado")
    }
)
public class ColaInscripcion {

    public enum Estado { PENDIENTE, PROMOVIDO, EXPIRADO, CANCELADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cola")
    private Long idCola;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_materia", nullable = false)
    private Materia materia;

    /** Número de orden en la cola (1 = primero) */
    @Column(name = "posicion", nullable = false)
    private Integer posicion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    /** Cuándo caduca la posición en la cola (por defecto: 48 horas) */
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    /** Cuándo fue promovido (inscripto) o expirado */
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    public ColaInscripcion() {}

    public ColaInscripcion(Estudiante estudiante, Materia materia,
                           Integer posicion, LocalDateTime fechaExpiracion) {
        this.estudiante      = estudiante;
        this.materia         = materia;
        this.posicion        = posicion;
        this.fechaSolicitud  = LocalDateTime.now();
        this.fechaExpiracion = fechaExpiracion;
        this.estado          = Estado.PENDIENTE;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public Long getIdCola()                       { return idCola; }
    public void setIdCola(Long v)                 { this.idCola = v; }

    public Estudiante getEstudiante()             { return estudiante; }
    public void setEstudiante(Estudiante v)       { this.estudiante = v; }

    public Materia getMateria()                   { return materia; }
    public void setMateria(Materia v)             { this.materia = v; }

    public Integer getPosicion()                  { return posicion; }
    public void setPosicion(Integer v)            { this.posicion = v; }

    public Estado getEstado()                     { return estado; }
    public void setEstado(Estado v)               { this.estado = v; }

    public LocalDateTime getFechaSolicitud()      { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime v){ this.fechaSolicitud = v; }

    public LocalDateTime getFechaExpiracion()     { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime v){ this.fechaExpiracion = v; }

    public LocalDateTime getFechaResolucion()     { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime v){ this.fechaResolucion = v; }
}
