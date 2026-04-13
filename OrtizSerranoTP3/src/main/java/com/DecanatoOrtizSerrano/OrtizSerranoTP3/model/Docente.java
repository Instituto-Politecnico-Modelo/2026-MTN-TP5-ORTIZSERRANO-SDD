package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "docentes")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Docente extends Usuario {
    
    @Column(name = "titulo", length = 200)
    private String titulo;
    
    @Column(name = "especialidad", length = 100)
    private String especialidad;
    
    @Column(name = "departamento", length = 100)
    private String departamento;
    
    @OneToMany(mappedBy = "docente", cascade = CascadeType.ALL)
    private List<Materia> materias = new ArrayList<>();
    
    // Constructores
    public Docente() {
        super();
    }
    
    public Docente(String nombre, String apellido, String email, String password,
                   String titulo, String especialidad, String departamento) {
        super(nombre, apellido, email, password);
        this.titulo = titulo;
        this.especialidad = especialidad;
        this.departamento = departamento;
    }
    
    // Getters y Setters
    public String getTitulo() {
        return titulo;
    }
    
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    
    public String getEspecialidad() {
        return especialidad;
    }
    
    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }
    
    public String getDepartamento() {
        return departamento;
    }
    
    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }
    
    public List<Materia> getMaterias() {
        return materias;
    }
    
    public void setMaterias(List<Materia> materias) {
        this.materias = materias;
    }
    
    // Métodos auxiliares
    public void addMateria(Materia materia) {
        materias.add(materia);
        materia.setDocente(this);
    }
    
    public void removeMateria(Materia materia) {
        materias.remove(materia);
        materia.setDocente(null);
    }
}
