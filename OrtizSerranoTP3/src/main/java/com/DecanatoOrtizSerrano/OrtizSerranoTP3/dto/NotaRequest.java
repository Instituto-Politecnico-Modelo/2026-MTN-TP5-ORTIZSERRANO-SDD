package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * DTO para que el docente cargue/actualice notas de una inscripción.
 */
public class NotaRequest {

    @DecimalMin(value = "0.0", message = "La nota parcial 1 debe ser >= 0")
    @DecimalMax(value = "10.0", message = "La nota parcial 1 debe ser <= 10")
    private Double notaParcial1;

    @DecimalMin(value = "0.0", message = "La nota parcial 2 debe ser >= 0")
    @DecimalMax(value = "10.0", message = "La nota parcial 2 debe ser <= 10")
    private Double notaParcial2;

    @DecimalMin(value = "0.0", message = "La nota final debe ser >= 0")
    @DecimalMax(value = "10.0", message = "La nota final debe ser <= 10")
    private Double notaFinal;

    private Integer asistencias;

    // Getters y Setters
    public Double getNotaParcial1() { return notaParcial1; }
    public void setNotaParcial1(Double notaParcial1) { this.notaParcial1 = notaParcial1; }

    public Double getNotaParcial2() { return notaParcial2; }
    public void setNotaParcial2(Double notaParcial2) { this.notaParcial2 = notaParcial2; }

    public Double getNotaFinal() { return notaFinal; }
    public void setNotaFinal(Double notaFinal) { this.notaFinal = notaFinal; }

    public Integer getAsistencias() { return asistencias; }
    public void setAsistencias(Integer asistencias) { this.asistencias = asistencias; }
}
