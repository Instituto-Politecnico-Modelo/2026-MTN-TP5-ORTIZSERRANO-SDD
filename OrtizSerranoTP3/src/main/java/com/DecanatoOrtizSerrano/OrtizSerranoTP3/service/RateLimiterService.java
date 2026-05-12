package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ─── Capa 1: Rate Limiter (Token Bucket por usuario) ───────────────────────
 *
 * Limita cuántas veces un mismo usuario puede intentar inscribirse
 * dentro de una ventana de tiempo deslizante.
 *
 * Algoritmo: Token Bucket simplificado (ventana fija por usuario).
 *   - Cada usuario tiene un "cubo" que se recarga cada {@code windowMs} ms.
 *   - Cada intento consume un token.
 *   - Si no hay tokens → HTTP 429 Too Many Requests.
 *
 * Configuración (application.properties):
 *   inscripcion.ratelimit.max-intentos=5      # tokens por ventana
 *   inscripcion.ratelimit.ventana-segundos=60  # duración de la ventana
 *
 * No requiere Redis ni dependencias externas. Almacenamiento en memoria;
 * se limpia automáticamente con la tarea programada en InscripcionService.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    @Value("${inscripcion.ratelimit.max-intentos:5}")
    private int maxIntentos;

    @Value("${inscripcion.ratelimit.ventana-segundos:60}")
    private int ventanaSegundos;

    /** Registro por usuario: [tokens restantes, timestamp inicio de ventana] */
    private final ConcurrentHashMap<Long, long[]> buckets = new ConcurrentHashMap<>();

    /**
     * Intenta consumir un token para el usuario dado.
     *
     * @param idUsuario ID del usuario autenticado
     * @return true si el intento está permitido, false si fue bloqueado (429)
     */
    public boolean tryConsume(Long idUsuario) {
        long ahora   = Instant.now().getEpochSecond();
        long ventana = ventanaSegundos;

        // getOrDefault + putIfAbsent para inicializar atómicamente
        long[] bucket = buckets.compute(idUsuario, (id, existing) -> {
            if (existing == null || (ahora - existing[1]) >= ventana) {
                // Nueva ventana: recarga tokens
                return new long[]{ maxIntentos, ahora };
            }
            return existing;
        });

        // bucket[0] = tokens disponibles
        synchronized (bucket) {
            if (bucket[0] > 0) {
                bucket[0]--;
                log.debug("RateLimit: usuario {} consumió token. Restantes: {}", idUsuario, bucket[0]);
                return true;
            } else {
                long espera = ventana - (ahora - bucket[1]);
                log.warn("RateLimit: usuario {} bloqueado. Debe esperar {}s", idUsuario, espera);
                return false;
            }
        }
    }

    /**
     * Cuántos segundos faltan para que se recarguen los tokens del usuario.
     * Retorna 0 si ya se puede intentar.
     */
    public long segundosHastaRecarga(Long idUsuario) {
        long[] bucket = buckets.get(idUsuario);
        if (bucket == null) return 0;
        long ahora = Instant.now().getEpochSecond();
        long transcurrido = ahora - bucket[1];
        return Math.max(0, ventanaSegundos - transcurrido);
    }

    /**
     * Tokens restantes para el usuario en la ventana actual.
     */
    public int tokensRestantes(Long idUsuario) {
        long[] bucket = buckets.get(idUsuario);
        if (bucket == null) return maxIntentos;
        long ahora = Instant.now().getEpochSecond();
        if ((ahora - bucket[1]) >= ventanaSegundos) return maxIntentos;
        synchronized (bucket) {
            return (int) bucket[0];
        }
    }

    /**
     * Limpia entradas inactivas (ventana vencida) para evitar memory leak.
     * Llamado periódicamente por InscripcionScheduler.
     */
    public void limpiarEntradas() {
        long ahora = Instant.now().getEpochSecond();
        int antes = buckets.size();
        buckets.entrySet().removeIf(e -> (ahora - e.getValue()[1]) >= ventanaSegundos * 2L);
        loginBuckets.entrySet().removeIf(e -> (ahora - e.getValue()[1]) >= LOGIN_WINDOW_SECONDS * 2L);
        int eliminados = antes - buckets.size();
        if (eliminados > 0) {
            log.debug("RateLimit: limpieza de {} entradas inactivas", eliminados);
        }
    }

    // ─── Rate Limit para Login (por IP) ──────────────────────────────────────

    /** Máximo de intentos de login fallidos por IP antes de bloquear. */
    private static final int    LOGIN_MAX_INTENTOS    = 10;
    /** Ventana de bloqueo tras superar el límite (segundos). */
    private static final long   LOGIN_WINDOW_SECONDS  = 300; // 5 minutos

    /** Registro por IP: [intentos fallidos, timestamp inicio ventana] */
    private final ConcurrentHashMap<String, long[]> loginBuckets = new ConcurrentHashMap<>();

    /**
     * Registra un intento de login fallido para la IP dada.
     * Llama a este método desde AuthController cuando la autenticación falla.
     */
    public void registrarLoginFallido(String ip) {
        long ahora = Instant.now().getEpochSecond();
        loginBuckets.compute(ip, (k, existing) -> {
            if (existing == null || (ahora - existing[1]) >= LOGIN_WINDOW_SECONDS) {
                return new long[]{ 1, ahora };
            }
            existing[0]++;
            return existing;
        });
        log.warn("RateLimit login: IP {} → intento fallido #{}", ip,
                loginBuckets.getOrDefault(ip, new long[]{0})[0]);
    }

    /**
     * Resetea el contador de intentos fallidos para una IP (tras login exitoso).
     */
    public void resetLoginFallidos(String ip) {
        loginBuckets.remove(ip);
    }

    /**
     * Indica si la IP está bloqueada por exceso de intentos fallidos.
     * @return true si debe bloquearse (HTTP 429)
     */
    public boolean loginBloqueado(String ip) {
        long[] bucket = loginBuckets.get(ip);
        if (bucket == null) return false;
        long ahora = Instant.now().getEpochSecond();
        if ((ahora - bucket[1]) >= LOGIN_WINDOW_SECONDS) {
            loginBuckets.remove(ip);
            return false;
        }
        return bucket[0] >= LOGIN_MAX_INTENTOS;
    }

    /**
     * Segundos que faltan para que se desbloquee la IP.
     */
    public long segundosHastaDesbloqueoLogin(String ip) {
        long[] bucket = loginBuckets.get(ip);
        if (bucket == null) return 0;
        long ahora = Instant.now().getEpochSecond();
        return Math.max(0, LOGIN_WINDOW_SECONDS - (ahora - bucket[1]));
    }

    public int getMaxIntentos()       { return maxIntentos; }
    public int getVentanaSegundos()   { return ventanaSegundos; }
}
