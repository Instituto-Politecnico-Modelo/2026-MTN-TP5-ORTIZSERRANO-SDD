package com.DecanatoOrtizSerrano.OrtizSerranoTP3.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejo global de excepciones para devolver mensajes claros al frontend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura conflictos de versión (optimistic locking).
     * Ocurre cuando dos transacciones leen la misma versión de Materia e intentan
     * inscribir al mismo tiempo → la segunda falla aquí, evitando sobrecupos.
     */
    @ExceptionHandler({
        jakarta.persistence.OptimisticLockException.class,
        org.springframework.orm.ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<Map<String, String>> handleOptimisticLock(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "Conflicto de concurrencia: otro usuario modificó el recurso al mismo tiempo. Intentá de nuevo.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Captura errores de validación @Valid y devuelve los mensajes de cada campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String mensajes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        Map<String, Object> body = new HashMap<>();
        body.put("message", mensajes);
        body.put("errores", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (a, b) -> a
                )));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Captura cualquier excepción no controlada.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "Error interno: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
