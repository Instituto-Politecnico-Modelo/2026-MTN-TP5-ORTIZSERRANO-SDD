package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body para POST /api/admin/usuarios/solicitudes-reset/{id}/atender */
public class AtenderResetRequest {

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String nuevaPassword;

    public AtenderResetRequest() {}
    public AtenderResetRequest(String nuevaPassword) { this.nuevaPassword = nuevaPassword; }

    public String getNuevaPassword() { return nuevaPassword; }
    public void setNuevaPassword(String nuevaPassword) { this.nuevaPassword = nuevaPassword; }
}
