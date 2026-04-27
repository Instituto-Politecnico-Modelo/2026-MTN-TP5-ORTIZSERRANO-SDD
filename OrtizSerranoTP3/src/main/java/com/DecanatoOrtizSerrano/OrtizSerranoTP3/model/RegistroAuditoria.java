package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro de auditoría encadenada (Blockchain-style).
 *
 * Cada entrada contiene:
 *  - Datos del evento (entidad, acción, usuario, descripción, IP)
 *  - hashAnterior: hash del registro previo en la cadena
 *  - hashActual:   SHA-256(hashAnterior + entidad + idEntidad + accion + descripcion + timestamp + usuarioId)
 *
 * Si alguien modifica un registro, su hashActual ya no coincidirá
 * con el hashAnterior del siguiente → la cadena queda rota → detectable.
 */
@Entity
@Table(
    name = "auditoria",
    indexes = {
        // Consultas por entidad específica (ej: historial de una inscripción)
        @Index(name = "idx_auditoria_entidad_id",   columnList = "entidad, id_entidad"),
        // Consultas por usuario (quién hizo qué)
        @Index(name = "idx_auditoria_usuario",      columnList = "id_usuario"),
        // Consultas por rango de tiempo (reportes, monitoring)
        @Index(name = "idx_auditoria_timestamp",    columnList = "timestamp_evento"),
        // Consultas por acción (ej: todos los LOGIN fallidos)
        @Index(name = "idx_auditoria_accion",       columnList = "accion")
    }
)
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long idRegistro;

    /** Nombre de la entidad afectada (ej: "Inscripcion", "Materia", "Usuario") */
    @Column(name = "entidad", nullable = false, length = 100)
    private String entidad;

    /** ID del objeto afectado */
    @Column(name = "id_entidad")
    private Long idEntidad;

    /** Acción realizada: CREAR, MODIFICAR, ELIMINAR, LOGIN, CERRAR_NOTA, etc. */
    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    /** ID del usuario que realizó la acción (null = sistema) */
    @Column(name = "id_usuario")
    private Long idUsuario;

    /** Email del usuario para trazabilidad sin JOIN */
    @Column(name = "email_usuario", length = 255)
    private String emailUsuario;

    /** Descripción legible del cambio */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /** IP del cliente (si aplica) */
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    /** Timestamp del evento */
    @Column(name = "timestamp_evento", nullable = false)
    private LocalDateTime timestampEvento;

    /**
     * Hash SHA-256 del registro anterior en la cadena.
     * Para el primer registro es "GENESIS".
     */
    @Column(name = "hash_anterior", nullable = false, length = 64)
    private String hashAnterior;

    /**
     * Hash SHA-256 de: hashAnterior + entidad + idEntidad + accion
     *                 + descripcion + timestamp + idUsuario
     * Garantiza integridad: si se modifica cualquier campo, el hash no coincide.
     */
    @Column(name = "hash_actual", nullable = false, unique = true, length = 64)
    private String hashActual;

    public RegistroAuditoria() {}

    // ─── Getters y Setters ────────────────────────────────────────────────────

    public Long getIdRegistro() { return idRegistro; }
    public void setIdRegistro(Long idRegistro) { this.idRegistro = idRegistro; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public String getEmailUsuario() { return emailUsuario; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario = emailUsuario; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }

    public LocalDateTime getTimestampEvento() { return timestampEvento; }
    public void setTimestampEvento(LocalDateTime timestampEvento) { this.timestampEvento = timestampEvento; }

    public String getHashAnterior() { return hashAnterior; }
    public void setHashAnterior(String hashAnterior) { this.hashAnterior = hashAnterior; }

    public String getHashActual() { return hashActual; }
    public void setHashActual(String hashActual) { this.hashActual = hashActual; }
}
