package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

/**
 * DTO para respuestas de mensajes
 */
public class MessageResponse {
    
    private String message;
    
    public MessageResponse(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
