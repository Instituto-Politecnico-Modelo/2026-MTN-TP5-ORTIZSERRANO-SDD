package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.ColaInscripcionResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.InscripcionRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Inscripcion;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.security.UserDetailsImpl;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.ColaInscripcionService;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.InscripcionService;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.MateriaSemaphoreService;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Inscripciones del estudiante autenticado.
 */
@Tag(name = "Inscripciones", description = "Inscripción de estudiantes a materias")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    @Autowired
    private InscripcionService inscripcionService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private ColaInscripcionService colaInscripcionService;

    @Autowired
    private MateriaSemaphoreService materiaSemaphoreService;

    /**
     * GET /api/inscripciones/mis-inscripciones
     * Devuelve todas las inscripciones del estudiante autenticado.
     */
    @Operation(summary = "Mis inscripciones", description = "Lista todas las inscripciones del estudiante autenticado.")
    @GetMapping("/mis-inscripciones")
    public ResponseEntity<?> misInscripciones(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("No autenticado"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<Inscripcion> inscripciones = inscripcionService.misInscripciones(userDetails.getId());
        return ResponseEntity.ok(inscripciones);
    }

    /**
     * POST /api/inscripciones
     * Inscribe al estudiante autenticado en una materia.
     */
    @Operation(
        summary = "Inscribirse a una materia",
        description = "El estudiante autenticado solicita inscripción en la materia indicada por `idMateria`. "
                    + "Retorna error 400 si ya está inscripto."
    )
    @PostMapping
    public ResponseEntity<?> inscribirse(
            @Valid @RequestBody InscripcionRequest request,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("No autenticado"));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // ── Capa 1: Rate Limiter ──────────────────────────────────────────────
        if (!rateLimiterService.tryConsume(userId)) {
            long espera = rateLimiterService.segundosHastaRecarga(userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(espera))
                .body(new MessageResponse(
                    "Demasiadas solicitudes. Por favor, esperá " + espera
                    + " segundos antes de reintentar.",
                    espera));
        }

        Long idMateria = request.getIdMateria();

        // ── Capa 2: Semáforo por materia ──────────────────────────────────────
        // Garantiza que solo N hilos (configurable) puedan realizar la
        // verificación de cupos + inserción simultáneamente para la misma materia.
        // Con permits=1 se serializa completamente, evitando race conditions.
        if (!materiaSemaphoreService.tryAcquire(idMateria)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "3")
                .body(new MessageResponse(
                    "El servidor está procesando muchas inscripciones. Reintentá en unos segundos.", 3L));
        }

        try {
            Inscripcion inscripcion = inscripcionService.inscribir(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(inscripcion);
        } catch (RuntimeException e) {
            String msg = e.getMessage();

            // ── Sin cupos disponibles → encolar automáticamente ──────────────
            if (msg != null && msg.contains("No hay cupos")) {
                try {
                    ColaInscripcionResponse turno =
                        colaInscripcionService.unirseACola(idMateria, userId);
                    // HTTP 202 Accepted: solicitud recibida pero no completada aún
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(turno);
                } catch (RuntimeException colaEx) {
                    return ResponseEntity.badRequest()
                        .body(new MessageResponse(colaEx.getMessage()));
                }
            }

            return ResponseEntity.badRequest().body(new MessageResponse(msg));
        } finally {
            // Siempre liberar el semáforo, incluso si hubo excepción
            materiaSemaphoreService.release(idMateria);
        }
    }

    /**
     * PATCH /api/inscripciones/{id}/cancelar
     * El estudiante cancela su propia inscripción.
     * Después del commit de la cancelación, promueve al primero de la cola
     * (en su propia TX independiente, para que countCuposOcupados sea correcto).
     */
    @Operation(summary = "Cancelar inscripción", description = "Cancela la inscripción indicada. Solo puede cancelar el propio estudiante.")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("No autenticado"));
        }
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Inscripcion inscripcion = inscripcionService.cancelar(id, userDetails.getId());

            // promoverSiguiente corre AQUÍ (fuera de la TX de cancelar, ya commiteada)
            // para que el COUNT de cupos ocupados refleje la cancelación persistida.
            colaInscripcionService.promoverSiguiente(
                inscripcion.getMateria().getIdMateria());

            return ResponseEntity.ok(inscripcion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/admin/inscripciones → Todas (solo admin, ver AdminController o SecurityConfig)
     */
    @Operation(summary = "Todas las inscripciones (admin)", description = "Lista todas las inscripciones del sistema.")
    @GetMapping("/todas")
    public ResponseEntity<List<Inscripcion>> listarTodas() {
        return ResponseEntity.ok(inscripcionService.listarTodas());
    }

    /**
     * GET /api/inscripciones/mis-notas
     * Devuelve las inscripciones del estudiante autenticado que tienen nota cargada.
     * Incluye nota parcial 1, parcial 2, nota final, asistencias y estado de cierre.
     */
    @Operation(
        summary = "Mis notas (boletín)",
        description = "Devuelve todas las inscripciones con nota del estudiante autenticado. "
                    + "Incluye parciales, nota final, asistencias y si la nota está cerrada."
    )
    @GetMapping("/mis-notas")
    public ResponseEntity<?> misNotas(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("No autenticado"));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<Inscripcion> inscripciones = inscripcionService.misInscripciones(userDetails.getId());
        // Filtramos solo las que tienen alguna nota o estado cerrado
        List<Inscripcion> conNota = inscripciones.stream()
            .filter(i -> i.getNotaFinal() != null
                      || i.getNotaParcial1() != null
                      || i.getNotaParcial2() != null
                      || i.isNotaCerrada())
            .toList();
        return ResponseEntity.ok(conNota);
    }
}
