package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

/**
 * DTO para respuestas JWT
 */
public class JwtResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    
    public JwtResponse(String accessToken, Long id, String email, String nombre, String apellido) {
        this.token = accessToken;
        this.id = id;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
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
}
