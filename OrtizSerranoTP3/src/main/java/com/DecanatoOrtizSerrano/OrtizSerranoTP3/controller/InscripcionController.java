package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.InscripcionRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Inscripcion;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.security.UserDetailsImpl;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.InscripcionService;
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
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Inscripcion inscripcion = inscripcionService.inscribir(request, userDetails.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(inscripcion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * PATCH /api/inscripciones/{id}/cancelar
     * El estudiante cancela su propia inscripción.
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
