package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

/**
 * DTO para respuestas de mensajes.
 * El campo {@code retryAfterSeconds} es opcional: se incluye cuando el servidor
 * emite HTTP 429 para indicarle al cliente cuánto debe esperar.
 */
public class MessageResponse {

    private String message;

    /** Segundos sugeridos de espera (solo presente en respuestas 429). */
    private Long retryAfterSeconds;

    public MessageResponse(String message) {
        this.message = message;
    }

    public MessageResponse(String message, long retryAfterSeconds) {
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(Long retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; }
}
