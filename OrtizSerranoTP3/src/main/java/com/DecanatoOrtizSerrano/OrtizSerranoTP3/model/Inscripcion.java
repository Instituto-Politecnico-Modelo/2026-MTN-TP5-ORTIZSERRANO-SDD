package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@Entity
@Table(
    name = "inscripciones",
    indexes = {
        // Búsqueda por estudiante (mis-inscripciones) → O(log n)
        @Index(name = "idx_inscripciones_estudiante",   columnList = "id_estudiante"),
        // Búsqueda por materia → O(log n)
        @Index(name = "idx_inscripciones_materia",      columnList = "id_materia"),
        // Consulta compuesta para verificar duplicado (estudiante + materia)
        @Index(name = "idx_inscripciones_est_mat",      columnList = "id_estudiante, id_materia"),
        // Consulta compuesta para COUNT de cupos: WHERE id_materia=? AND estado != 'CANCELADA'
        @Index(name = "idx_inscripciones_mat_estado",   columnList = "id_materia, estado"),
        // Filtrado por estado (reportes, auditoría)
        @Index(name = "idx_inscripciones_estado",       columnList = "estado")
    }
)
public class Inscripcion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscripcion")
    private Long idInscripcion;

    // ignora la lista de inscripciones del estudiante para evitar ciclo JSON infinito
    @JsonIgnoreProperties({"inscripciones", "password"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    // ignora la lista de inscripciones de la materia para evitar ciclo JSON infinito
    @JsonIgnoreProperties({"inscripciones"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_materia", nullable = false)
    private Materia materia;
    
    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;
    
    @Column(name = "estado", nullable = false, length = 20)
    private String estado; // ACTIVA, APROBADA, DESAPROBADA, CANCELADA
    
    @Column(name = "nota_final")
    private Double notaFinal;
    
    @Column(name = "nota_parcial1")
    private Double notaParcial1;
    
    @Column(name = "nota_parcial2")
    private Double notaParcial2;
    
    @Column(name = "asistencias")
    private Integer asistencias;

    /**
     * Indica si la nota fue cerrada definitivamente por el docente.
     * Una vez true, no se pueden modificar ni nota parcial, ni final, ni asistencias.
     */
    @Column(name = "nota_cerrada", nullable = false)
    private boolean notaCerrada = false;

    // Constructores
    public Inscripcion() {
    }
    
    public Inscripcion(Estudiante estudiante, Materia materia, LocalDate fechaInscripcion) {
        this.estudiante = estudiante;
        this.materia = materia;
        this.fechaInscripcion = fechaInscripcion;
        this.estado = "ACTIVA";
    }
    
    // Getters y Setters
    public Long getIdInscripcion() {
        return idInscripcion;
    }
    
    public void setIdInscripcion(Long idInscripcion) {
        this.idInscripcion = idInscripcion;
    }
    
    public Estudiante getEstudiante() {
        return estudiante;
    }
    
    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }
    
    public Materia getMateria() {
        return materia;
    }
    
    public void setMateria(Materia materia) {
        this.materia = materia;
    }
    
    public LocalDate getFechaInscripcion() {
        return fechaInscripcion;
    }
    
    public void setFechaInscripcion(LocalDate fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public Double getNotaFinal() {
        return notaFinal;
    }
    
    public void setNotaFinal(Double notaFinal) {
        this.notaFinal = notaFinal;
    }
    
    public Double getNotaParcial1() {
        return notaParcial1;
    }
    
    public void setNotaParcial1(Double notaParcial1) {
        this.notaParcial1 = notaParcial1;
    }
    
    public Double getNotaParcial2() {
        return notaParcial2;
    }
    
    public void setNotaParcial2(Double notaParcial2) {
        this.notaParcial2 = notaParcial2;
    }
    
    public Integer getAsistencias() {
        return asistencias;
    }
    
    public void setAsistencias(Integer asistencias) {
        this.asistencias = asistencias;
    }

    public boolean isNotaCerrada() {
        return notaCerrada;
    }

    public void setNotaCerrada(boolean notaCerrada) {
        this.notaCerrada = notaCerrada;
    }

    // Métodos auxiliares
    public Double calcularPromedio() {
        if (notaParcial1 != null && notaParcial2 != null) {
            return (notaParcial1 + notaParcial2) / 2.0;
        }
        return null;
    }
    
    public boolean estaAprobada() {
        return notaFinal != null && notaFinal >= 6.0;
    }
}
