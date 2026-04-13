package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;

@Entity
@Table(name = "administradores")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Administrador extends Usuario {
    
    @Column(name = "rol", nullable = false, length = 50)
    private String rol;
    
    @Column(name = "area", length = 100)
    private String area;
    
    @Column(name = "nivel_acceso")
    private Integer nivelAcceso;
    
    // Constructores
    public Administrador() {
        super();
    }
    
    public Administrador(String nombre, String apellido, String email, String password,
                        String rol, String area, Integer nivelAcceso) {
        super(nombre, apellido, email, password);
        this.rol = rol;
        this.area = area;
        this.nivelAcceso = nivelAcceso;
    }
    
    // Getters y Setters
    public String getRol() {
        return rol;
    }
    
    public void setRol(String rol) {
        this.rol = rol;
    }
    
    public String getArea() {
        return area;
    }
    
    public void setArea(String area) {
        this.area = area;
    }
    
    public Integer getNivelAcceso() {
        return nivelAcceso;
    }
    
    public void setNivelAcceso(Integer nivelAcceso) {
        this.nivelAcceso = nivelAcceso;
    }
}
