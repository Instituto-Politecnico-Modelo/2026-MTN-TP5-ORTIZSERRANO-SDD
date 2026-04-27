package com.DecanatoOrtizSerrano.OrtizSerranoTP3.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
     * Captura errores de autenticación (credenciales inválidas) → 401.
     * Sin este handler, Spring Boot 4 los propaga como 500.
     */
    @ExceptionHandler({
        org.springframework.security.authentication.BadCredentialsException.class,
        org.springframework.security.core.AuthenticationException.class
    })
    public ResponseEntity<Map<String, String>> handleAuthenticationException(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "Credenciales inválidas");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Captura violaciones de acceso por rol insuficiente (@PreAuthorize) → 403.
     * Sin este handler, AccessDeniedException era capturada por handleGenericException → 500.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "Acceso denegado: no tenés permiso para realizar esta acción");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Captura JSON malformado / no legible en el body → 400.
     * Sin este handler, Jackson lanzaba excepción capturada como 500.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedJson(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "El body de la petición no es un JSON válido");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Captura errores de conversión de tipo en path variables (ej: String donde se espera Long) → 400.
     * Evita 500 ante inyecciones en parámetros de ruta.
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "Parámetro inválido: '" + ex.getName() + "' debe ser de tipo "
            + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "correcto"));
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
