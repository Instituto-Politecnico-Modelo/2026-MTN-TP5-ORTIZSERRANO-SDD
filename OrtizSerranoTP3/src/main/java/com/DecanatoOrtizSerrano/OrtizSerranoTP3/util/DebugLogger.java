package com.DecanatoOrtizSerrano.OrtizSerranoTP3.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad de Debugging
 * Proporciona métodos útiles para debugging durante el desarrollo
 */
@Component
public class DebugLogger {

    private static final Logger logger = LoggerFactory.getLogger(DebugLogger.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Registra información de debugging
     */
    public static void debug(String message) {
        logger.debug("[{}] {}", LocalDateTime.now().format(formatter), message);
    }

    /**
     * Registra información de debugging con parámetros
     */
    public static void debug(String message, Object... params) {
        logger.debug("[{}] " + message, addTimestamp(params));
    }

    /**
     * Registra el inicio de un método
     */
    public static void methodStart(String className, String methodName, Object... params) {
        logger.debug("▶️  INICIO - {}.{}() - Parámetros: {}", className, methodName, formatParams(params));
    }

    /**
     * Registra el fin de un método
     */
    public static void methodEnd(String className, String methodName, Object result) {
        logger.debug("⏹️  FIN - {}.{}() - Resultado: {}", className, methodName, result);
    }

    /**
     * Registra una excepción con contexto
     */
    public static void exception(String context, Exception e) {
        logger.error("❌ EXCEPCIÓN en {}: {} - {}", context, e.getClass().getSimpleName(), e.getMessage(), e);
    }

    /**
     * Registra el estado de un objeto
     */
    public static void objectState(String objectName, Object object) {
        logger.debug("📦 Estado de {}: {}", objectName, object != null ? object.toString() : "null");
    }

    /**
     * Registra una consulta SQL (adicional al logging de Hibernate)
     */
    public static void sql(String query, Object... params) {
        logger.debug("🗄️  SQL: {} | Parámetros: {}", query, formatParams(params));
    }

    /**
     * Marca un checkpoint en el código
     */
    public static void checkpoint(String checkpointName) {
        logger.debug("🚩 CHECKPOINT: {}", checkpointName);
    }

    /**
     * Registra datos de performance
     */
    public static void performance(String operation, long milliseconds) {
        logger.debug("⏱️  PERFORMANCE - {} tomó {} ms", operation, milliseconds);
    }

    // Métodos auxiliares privados
    private static Object[] addTimestamp(Object[] params) {
        Object[] newParams = new Object[params.length + 1];
        newParams[0] = LocalDateTime.now().format(formatter);
        System.arraycopy(params, 0, newParams, 1, params.length);
        return newParams;
    }

    private static String formatParams(Object... params) {
        if (params == null || params.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i]);
            if (i < params.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
