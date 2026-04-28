package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de lista negra de tokens JWT.
 *
 * Mantiene un mapa en memoria { token → instante de expiración }.
 * El filtro JWT consulta este servicio antes de aceptar un token.
 * Una tarea programada purga automáticamente los tokens ya expirados
 * para evitar crecimiento indefinido del mapa.
 *
 * Nota: al ser in-memory, la blacklist se reinicia con cada restart.
 * Para producción se reemplazaría por Redis con TTL automático.
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    /** token → momento en que expira (para poder purgarlo después) */
    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();

    /**
     * Añade un token a la lista negra.
     *
     * @param token     el JWT a invalidar
     * @param expiresAt instante en que el token expira de forma natural;
     *                  pasado ese momento puede ser purgado del mapa
     */
    public void invalidate(String token, Instant expiresAt) {
        blacklist.put(token, expiresAt);
        logger.info("Token agregado a la blacklist. Expirará en: {}", expiresAt);
    }

    /**
     * Consulta si un token fue invalidado (está en la blacklist).
     *
     * @param token el JWT a comprobar
     * @return {@code true} si el token fue revocado por logout
     */
    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * Tarea programada: cada hora elimina del mapa los tokens cuya
     * expiración natural ya pasó (no necesitan seguir en la blacklist
     * porque el propio filtro los rechazaría por expirados de todos modos).
     */
    @Scheduled(fixedDelay = 3_600_000)   // cada 1 hora
    public void purgarTokensExpirados() {
        Instant ahora = Instant.now();
        int antes = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(ahora));
        int despues = blacklist.size();
        if (antes != despues) {
            logger.info("Blacklist purgada: {} tokens eliminados ({} restantes)", antes - despues, despues);
        }
    }

    /** Tamaño actual (para tests / monitoreo). */
    public int size() {
        return blacklist.size();
    }
}
