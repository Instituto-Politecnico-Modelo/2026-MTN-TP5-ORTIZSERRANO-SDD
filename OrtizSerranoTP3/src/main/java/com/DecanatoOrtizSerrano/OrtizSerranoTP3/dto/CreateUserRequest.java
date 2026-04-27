package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para que el administrador cree un nuevo usuario con un rol específico.
 * Roles válidos: ESTUDIANTE, DOCENTE, ADMINISTRADOR
 */
public class CreateUserRequest {

    // ── Campos obligatorios ───────────────────────────────────────────────

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100)
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 40)
    private String password;

    /**
     * Rol a asignar. Valores válidos: ESTUDIANTE, DOCENTE, ADMINISTRADOR
     */
    @NotBlank(message = "El rol es obligatorio")
    private String rol;

    // ── Campos opcionales para ESTUDIANTE ────────────────────────────────

    private String legajo;
    private String carrera;
    private Integer anioIngreso;

    // ── Campos opcionales para DOCENTE ───────────────────────────────────

    private String titulo;
    private String especialidad;
    private String departamento;

    // ── Campos opcionales para ADMINISTRADOR ─────────────────────────────

    private String area;
    private Integer nivelAcceso;

    // ── Getters y Setters ────────────────────────────────────────────────

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getLegajo() { return legajo; }
    public void setLegajo(String legajo) { this.legajo = legajo; }

    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }

    public Integer getAnioIngreso() { return anioIngreso; }
    public void setAnioIngreso(Integer anioIngreso) { this.anioIngreso = anioIngreso; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public Integer getNivelAcceso() { return nivelAcceso; }
    public void setNivelAcceso(Integer nivelAcceso) { this.nivelAcceso = nivelAcceso; }
}
