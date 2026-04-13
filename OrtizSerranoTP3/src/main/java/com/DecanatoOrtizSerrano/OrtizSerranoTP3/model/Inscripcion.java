package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "inscripciones")
public class Inscripcion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscripcion")
    private Long idInscripcion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;
    
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
