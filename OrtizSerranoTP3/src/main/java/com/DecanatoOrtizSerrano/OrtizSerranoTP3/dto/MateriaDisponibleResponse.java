package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

/**
 * DTO de respuesta que expone una materia con el estado de sus cupos calculado.
 *
 * Campos de cupos:
 *  - cuposMaximos:      límite configurado (null = sin límite)
 *  - cuposOcupados:     inscripciones activas (estado ≠ CANCELADA)
 *  - cuposDisponibles:  cuposMaximos - cuposOcupados  (null si no hay límite)
 *  - hayLugar:          true si se puede inscribir
 *  - porcentajeOcupacion: 0-100 (null si no hay límite)
 */
public class MateriaDisponibleResponse {

    private Long    idMateria;
    private String  codigo;
    private String  nombre;
    private String  descripcion;
    private Integer creditos;
    private Integer anio;
    private Integer cuatrimestre;

    // Docente
    private String docenteNombre;
    private String docenteApellido;
    private String docenteEmail;

    // Cupos
    private Integer cuposMaximos;
    private int     cuposOcupados;
    private Integer cuposDisponibles;    // null si sin límite
    private boolean hayLugar;
    private Integer porcentajeOcupacion; // null si sin límite

    public MateriaDisponibleResponse() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getIdMateria()               { return idMateria; }
    public void setIdMateria(Long v)         { this.idMateria = v; }

    public String getCodigo()                { return codigo; }
    public void setCodigo(String v)          { this.codigo = v; }

    public String getNombre()                { return nombre; }
    public void setNombre(String v)          { this.nombre = v; }

    public String getDescripcion()           { return descripcion; }
    public void setDescripcion(String v)     { this.descripcion = v; }

    public Integer getCreditos()             { return creditos; }
    public void setCreditos(Integer v)       { this.creditos = v; }

    public Integer getAnio()                 { return anio; }
    public void setAnio(Integer v)           { this.anio = v; }

    public Integer getCuatrimestre()         { return cuatrimestre; }
    public void setCuatrimestre(Integer v)   { this.cuatrimestre = v; }

    public String getDocenteNombre()         { return docenteNombre; }
    public void setDocenteNombre(String v)   { this.docenteNombre = v; }

    public String getDocenteApellido()       { return docenteApellido; }
    public void setDocenteApellido(String v) { this.docenteApellido = v; }

    public String getDocenteEmail()          { return docenteEmail; }
    public void setDocenteEmail(String v)    { this.docenteEmail = v; }

    public Integer getCuposMaximos()         { return cuposMaximos; }
    public void setCuposMaximos(Integer v)   { this.cuposMaximos = v; }

    public int getCuposOcupados()            { return cuposOcupados; }
    public void setCuposOcupados(int v)      { this.cuposOcupados = v; }

    public Integer getCuposDisponibles()     { return cuposDisponibles; }
    public void setCuposDisponibles(Integer v){ this.cuposDisponibles = v; }

    public boolean isHayLugar()              { return hayLugar; }
    public void setHayLugar(boolean v)       { this.hayLugar = v; }

    public Integer getPorcentajeOcupacion()  { return porcentajeOcupacion; }
    public void setPorcentajeOcupacion(Integer v){ this.porcentajeOcupacion = v; }
}
