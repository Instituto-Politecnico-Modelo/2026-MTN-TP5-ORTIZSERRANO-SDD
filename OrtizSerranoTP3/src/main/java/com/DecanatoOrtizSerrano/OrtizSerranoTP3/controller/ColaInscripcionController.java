package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.ColaInscripcionResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.security.UserDetailsImpl;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.ColaInscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de la cola virtual de inscripciones.
 *
 *  POST   /api/inscripciones/cola                        → unirse a la cola
 *  DELETE /api/inscripciones/cola/{id}                   → abandonar turno
 *  GET    /api/inscripciones/cola/mis-turnos              → mis turnos activos
 *  GET    /api/inscripciones/cola/materia/{idMateria}     → cola de una materia (admin)
 */
@Tag(name = "Cola de Inscripciones", description = "Fila virtual por cupos — gestión de turnos FIFO")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/inscripciones/cola")
public class ColaInscripcionController {

    @Autowired
    private ColaInscripcionService colaService;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/inscripciones/cola?idMateria={id}
     * Agrega al estudiante autenticado a la cola de una materia llena.
     * Retorna su posición y cuántas personas tiene delante.
     */
    @Operation(
        summary     = "Unirse a la cola de una materia",
        description = "El estudiante se agrega a la lista de espera FIFO de una materia. "
                    + "Se invoca cuando POST /api/inscripciones devuelve HTTP 202."
    )
    @PostMapping
    public ResponseEntity<?> unirse(
            @RequestParam Long idMateria,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("No autenticado"));
        }
        try {
            UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
            ColaInscripcionResponse resp = colaService.unirseACola(idMateria, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * DELETE /api/inscripciones/cola/{id}
     * El estudiante abandona voluntariamente su turno.
     */
    @Operation(
        summary     = "Abandonar turno en cola",
        description = "Cancela el turno PENDIENTE del estudiante en la lista de espera."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> abandonar(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("No autenticado"));
        }
        try {
            UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
            colaService.abandonarCola(id, user.getId());
            return ResponseEntity.ok(new MessageResponse("Turno cancelado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/inscripciones/cola/mis-turnos
     * Lista los turnos PENDIENTES del estudiante autenticado.
     */
    @Operation(
        summary     = "Mis turnos en cola",
        description = "Lista los turnos activos (PENDIENTE) del estudiante en todas las materias."
    )
    @GetMapping("/mis-turnos")
    public ResponseEntity<?> misTurnos(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("No autenticado"));
        }
        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        List<ColaInscripcionResponse> turnos = colaService.misColas(user.getId());
        return ResponseEntity.ok(turnos);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/inscripciones/cola/materia/{idMateria}
     * Cola completa de una materia (solo administradores).
     */
    @Operation(
        summary     = "Cola de una materia (admin)",
        description = "Lista todos los turnos PENDIENTES de la materia indicada, en orden FIFO."
    )
    @GetMapping("/materia/{idMateria}")
    public ResponseEntity<List<ColaInscripcionResponse>> colaDeMat(
            @PathVariable Long idMateria) {
        return ResponseEntity.ok(colaService.colaDeMat(idMateria));
    }
}
