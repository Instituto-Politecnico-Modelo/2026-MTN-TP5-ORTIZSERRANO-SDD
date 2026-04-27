package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(
    name = "usuarios",
    indexes = {
        // Login: findByEmail → la columna ya es UNIQUE, el índice único acelera el lookup
        @Index(name = "idx_usuarios_email",  columnList = "email",  unique = true),
        // Filtrado por activo (listar usuarios activos/inactivos)
        @Index(name = "idx_usuarios_activo", columnList = "activo")
    }
)
@Inheritance(strategy = InheritanceType.JOINED)
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;
    
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
    
    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public Usuario() {
        this.activo = true;
    }
    
    public Usuario(String nombre, String apellido, String email, String password) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getApellido() {
        return apellido;
    }
    
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Boolean getActivo() {
        return activo;
    }
    
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    public boolean isActivo() {
        return activo != null && activo;
    }
}
