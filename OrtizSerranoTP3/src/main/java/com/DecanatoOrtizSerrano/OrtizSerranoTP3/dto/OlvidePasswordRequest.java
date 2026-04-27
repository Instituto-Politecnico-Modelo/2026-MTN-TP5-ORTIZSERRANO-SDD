package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body para POST /api/auth/olvide-password */
public class OlvidePasswordRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    public OlvidePasswordRequest() {}
    public OlvidePasswordRequest(String email) { this.email = email; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
