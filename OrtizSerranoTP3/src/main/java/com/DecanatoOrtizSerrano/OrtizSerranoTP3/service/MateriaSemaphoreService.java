package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * ─── Capa 2: Semáforo por Materia ─────────────────────────────────────────
 *
 * Limita la cantidad de transacciones DB concurrentes para una misma materia.
 * Evita que muchos hilos compitan por el mismo registro Materia (cupos),
 * complementando el OptimisticLocking (@Version) ya existente.
 *
 * Configuración (application.properties):
 *   inscripcion.semaforo.permisos=3        # máx. threads simultáneos por materia
 *   inscripcion.semaforo.timeout-ms=3000   # tiempo máximo de espera para adquirir
 */
@Service
public class MateriaSemaphoreService {

    private static final Logger log = LoggerFactory.getLogger(MateriaSemaphoreService.class);

    @Value("${inscripcion.semaforo.permisos:3}")
    private int permisosDefault;

    @Value("${inscripcion.semaforo.timeout-ms:3000}")
    private long timeoutMs;

    private final ConcurrentHashMap<Long, Semaphore> semaforos = new ConcurrentHashMap<>();

    private Semaphore getSemaforo(Long idMateria) {
        return semaforos.computeIfAbsent(idMateria, id -> new Semaphore(permisosDefault, true));
    }

    /**
     * Intenta adquirir un permiso para la materia.
     * Bloquea hasta {@code timeoutMs} ms si no hay permisos disponibles.
     *
     * @param idMateria ID de la materia
     * @return true si se adquirió el permiso, false si venció el timeout
     */
    public boolean tryAcquire(Long idMateria) {
        Semaphore s = getSemaforo(idMateria);
        try {
            boolean ok = s.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            if (!ok) {
                log.warn("Semaforo: timeout al adquirir permiso para materia {}", idMateria);
            }
            return ok;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Semaforo: hilo interrumpido esperando permiso para materia {}", idMateria);
            return false;
        }
    }

    /**
     * Libera el permiso adquirido para la materia.
     * Debe llamarse siempre en un bloque finally.
     *
     * @param idMateria ID de la materia
     */
    public void release(Long idMateria) {
        Semaphore s = semaforos.get(idMateria);
        if (s != null) {
            s.release();
            log.debug("Semaforo: permiso liberado para materia {}", idMateria);
        }
    }

    /**
     * Cuántos permisos disponibles quedan para la materia.
     */
    public int permisosDisponibles(Long idMateria) {
        Semaphore s = semaforos.get(idMateria);
        return s == null ? permisosDefault : s.availablePermits();
    }
}
