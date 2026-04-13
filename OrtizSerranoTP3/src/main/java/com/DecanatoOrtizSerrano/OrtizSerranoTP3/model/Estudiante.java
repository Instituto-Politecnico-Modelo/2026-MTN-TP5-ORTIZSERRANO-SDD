package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estudiantes")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Estudiante extends Usuario {
    
    @Column(name = "legajo", nullable = false, unique = true, length = 20)
    private String legajo;
    
    @Column(name = "carrera", nullable = false, length = 100)
    private String carrera;
    
    @Column(name = "anio_ingreso")
    private Integer anioIngreso;
    
    @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones = new ArrayList<>();
    
    // Constructores
    public Estudiante() {
        super();
    }
    
    public Estudiante(String nombre, String apellido, String email, String password, 
                      String legajo, String carrera, Integer anioIngreso) {
        super(nombre, apellido, email, password);
        this.legajo = legajo;
        this.carrera = carrera;
        this.anioIngreso = anioIngreso;
    }
    
    // Getters y Setters
    public String getLegajo() {
        return legajo;
    }
    
    public void setLegajo(String legajo) {
        this.legajo = legajo;
    }
    
    public String getCarrera() {
        return carrera;
    }
    
    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }
    
    public Integer getAnioIngreso() {
        return anioIngreso;
    }
    
    public void setAnioIngreso(Integer anioIngreso) {
        this.anioIngreso = anioIngreso;
    }
    
    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }
    
    public void setInscripciones(List<Inscripcion> inscripciones) {
        this.inscripciones = inscripciones;
    }
    
    // Métodos auxiliares
    public void addInscripcion(Inscripcion inscripcion) {
        inscripciones.add(inscripcion);
        inscripcion.setEstudiante(this);
    }
    
    public void removeInscripcion(Inscripcion inscripcion) {
        inscripciones.remove(inscripcion);
        inscripcion.setEstudiante(null);
    }
}
