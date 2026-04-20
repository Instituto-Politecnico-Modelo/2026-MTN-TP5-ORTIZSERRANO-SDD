package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un espacio físico del Decanato (auditorio, aula magna, sala de reuniones, etc.)
 */
@Entity
@Table(name = "auditorios")
public class Auditorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditorio")
    private Long idAuditorio;

    @Column(name = "nombre", nullable = false, unique = true, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    /** Ubicación física dentro del establecimiento */
    @Column(name = "ubicacion", length = 200)
    private String ubicacion;

    /** Equipamiento disponible (proyector, pizarrón, sonido, etc.) */
    @Column(name = "equipamiento", columnDefinition = "TEXT")
    private String equipamiento;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "auditorio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservaAuditorio> reservas = new ArrayList<>();

    // ── Constructores ────────────────────────────────────────────────────────
    public Auditorio() {}

    public Auditorio(String nombre, Integer capacidad, String ubicacion) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.ubicacion = ubicacion;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getIdAuditorio() { return idAuditorio; }
    public void setIdAuditorio(Long idAuditorio) { this.idAuditorio = idAuditorio; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getEquipamiento() { return equipamiento; }
    public void setEquipamiento(String equipamiento) { this.equipamiento = equipamiento; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public List<ReservaAuditorio> getReservas() { return reservas; }
    public void setReservas(List<ReservaAuditorio> reservas) { this.reservas = reservas; }
}
