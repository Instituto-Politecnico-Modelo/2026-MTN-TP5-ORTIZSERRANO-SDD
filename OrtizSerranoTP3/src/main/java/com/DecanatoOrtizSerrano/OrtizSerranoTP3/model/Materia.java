package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "materias",
    indexes = {
        // Búsqueda por código (ya UNIQUE, el índice único lo cubre)
        @Index(name = "idx_materias_codigo",  columnList = "codigo",     unique = true),
        // Listar materias por docente
        @Index(name = "idx_materias_docente", columnList = "id_docente"),
        // Filtrar por año/cuatrimestre (oferta académica)
        @Index(name = "idx_materias_anio_cuatrimestre", columnList = "anio, cuatrimestre")
    }
)
public class Materia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_materia")
    private Long idMateria;

    /**
     * Control de concurrencia optimista.
     * Hibernate incrementa este valor en cada UPDATE.
     * Si dos transacciones leen el mismo valor y la segunda intenta actualizar,
     * falla con OptimisticLockException → evita sobrecupos.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "cupos_maximos")
    private Integer cuposMaximos;

    @Column(name = "carrera", length = 150)
    private String carrera;
    
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

    public Long getVersion() {
        return version;
    }

    public Integer getCuposMaximos() {
        return cuposMaximos;
    }

    public void setCuposMaximos(Integer cuposMaximos) {
        this.cuposMaximos = cuposMaximos;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    /** Cupos ocupados = inscripciones activas (no canceladas) */
    public int getCuposOcupados() {
        if (inscripciones == null) return 0;
        return (int) inscripciones.stream()
                .filter(i -> !"CANCELADA".equals(i.getEstado()))
                .count();
    }

    /** true si la materia tiene cupos disponibles (o no tiene límite definido) */
    public boolean tieneCuposDisponibles() {
        if (cuposMaximos == null || cuposMaximos <= 0) return true;
        return getCuposOcupados() < cuposMaximos;
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