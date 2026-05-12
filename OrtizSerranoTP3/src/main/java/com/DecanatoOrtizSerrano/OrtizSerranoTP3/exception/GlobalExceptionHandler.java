package com.DecanatoOrtizSerrano.OrtizSerranoTP3.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Manejo global de excepciones para devolver mensajes claros al frontend
 * sin filtrar información interna del servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    /** Clave estándar del campo de mensaje en todas las respuestas de error. */
    private static final String MSG_KEY = "message";

    /** Conflictos de versión (optimistic locking). */
    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, String>> handleOptimisticLock(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put(MSG_KEY, "Conflicto de concurrencia: otro usuario modificó el recurso al mismo tiempo. Intentá de nuevo.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /** Errores de validación @Valid. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String mensajes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        Map<String, Object> body = new HashMap<>();
        body.put(MSG_KEY, mensajes);
        body.put("errores", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (a, b) -> a
                )));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** Credenciales inválidas → 401. */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, String>> handleAuthenticationException(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put(MSG_KEY, "Credenciales inválidas");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /** Acceso denegado por rol insuficiente → 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> body = new HashMap<>();
        body.put(MSG_KEY, "Acceso denegado: no tenés permiso para realizar esta acción");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /** JSON malformado → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedJson(HttpMessageNotReadableException ex) {
        Map<String, String> body = new HashMap<>();
        body.put(MSG_KEY, "El body de la petición no es un JSON válido");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** Tipo incorrecto en path variable → 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> body = new HashMap<>();
        Class<?> requiredType = ex.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "correcto";
        body.put(MSG_KEY, "Parámetro inválido: '" + ex.getName() + "' debe ser de tipo " + typeName);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Captura cualquier RuntimeException no controlada.
     *
     * ⚠️  SEGURIDAD: se loguea el mensaje real del servidor (para debugging)
     * pero al cliente se le devuelve un mensaje genérico para evitar filtrar
     * información interna (nombres de tablas, emails de usuarios, stack traces, etc.).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        // Log interno con el mensaje real (visible solo en logs del servidor)
        log.error("[GlobalExceptionHandler] RuntimeException no controlada: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        // Al cliente: mensaje genérico que no filtra información interna
        body.put(MSG_KEY, "Se produjo un error al procesar la solicitud. Intentá de nuevo.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Captura cualquier otra excepción no controlada.
     * Mismo principio: loguear internamente, responder genéricamente al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("[GlobalExceptionHandler] Excepción no controlada: {}", ex.getMessage(), ex);
        Map<String, String> body = new HashMap<>();
        body.put(MSG_KEY, "Error interno del servidor. Por favor, intentá más tarde.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
