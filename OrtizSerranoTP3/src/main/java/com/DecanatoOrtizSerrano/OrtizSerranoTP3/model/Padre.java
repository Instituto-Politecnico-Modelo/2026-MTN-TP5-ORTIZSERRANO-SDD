package com.DecanatoOrtizSerrano.OrtizSerranoTP3.model;

import jakarta.persistence.*;

/**
 * Representa a un padre/tutor que puede consultar
 * el historial académico y de asistencias de un alumno.
 */
@Entity
@Table(name = "padres")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Padre extends Usuario {

    @Column(name = "dni", length = 20)
    private String dni;

    @Column(name = "telefono", length = 30)
    private String telefono;

    public Padre() {
        super();
    }

    public Padre(String nombre, String apellido, String email, String password) {
        super(nombre, apellido, email, password);
    }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
