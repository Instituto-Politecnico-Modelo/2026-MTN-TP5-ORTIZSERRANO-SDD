package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

import jakarta.validation.constraints.NotNull;

public class InscripcionRequest {

    @NotNull(message = "El ID de la materia es obligatorio")
    private Long idMateria;

    public Long getIdMateria() { return idMateria; }
    public void setIdMateria(Long idMateria) { this.idMateria = idMateria; }
}
