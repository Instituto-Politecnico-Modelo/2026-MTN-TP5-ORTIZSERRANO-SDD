package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "materias")
public class Materia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_materia")
    private Long idMateria;
    
    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;
    
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;
    
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
    
    @Column(name = "creditos")
    private Integer creditos;
    
    @Column(name = "anio")
    private Integer anio;
    
    @Column(name = "cuatrimestre")
    private Integer cuatrimestre;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente")
    private Docente docente;
    
    @OneToMany(mappedBy = "materia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones = new ArrayList<>();
    
    // Constructores
    public Materia() {
    }
    
    public Materia(String codigo, String nombre, String descripcion, Integer creditos) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creditos = creditos;
    }
    
    // Getters y Setters
    public Long getIdMateria() {
        return idMateria;
    }
    
    public void setIdMateria(Long idMateria) {
        this.idMateria = idMateria;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public Integer getCreditos() {
        return creditos;
    }
    
    public void setCreditos(Integer creditos) {
        this.creditos = creditos;
    }
    
    public Integer getAnio() {
        return anio;
    }
    
    public void setAnio(Integer anio) {
        this.anio = anio;
    }
    
    public Integer getCuatrimestre() {
        return cuatrimestre;
    }
    
    public void setCuatrimestre(Integer cuatrimestre) {
        this.cuatrimestre = cuatrimestre;
    }
    
    public Docente getDocente() {
        return docente;
    }
    
    public void setDocente(Docente docente) {
        this.docente = docente;
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
        inscripcion.setMateria(this);
    }
    
    public void removeInscripcion(Inscripcion inscripcion) {
        inscripciones.remove(inscripcion);
        inscripcion.setMateria(null);
    }
}