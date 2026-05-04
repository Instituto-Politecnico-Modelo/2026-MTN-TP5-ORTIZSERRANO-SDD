package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.NotaRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Inscripcion;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.InscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para el docente: gestión de notas de los alumnos.
 * Requiere rol DOCENTE o ADMINISTRADOR.
 */
@Tag(name = "Docente – Notas", description = "Carga y cierre de notas por parte del docente")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
@RestController
@RequestMapping("/api/docente")
@PreAuthorize("hasAnyAuthority('DOCENTE','ADMINISTRADOR')")
public class DocenteController {

    @Autowired
    private InscripcionService inscripcionService;

    /**
     * GET /api/docente/materias/{idMateria}/inscripciones
     * Lista todos los alumnos inscriptos en una materia (no cancelados).
     */
    @Operation(
        summary = "Alumnos de una materia",
        description = "Devuelve todas las inscripciones activas/con nota de la materia indicada."
    )
    @GetMapping("/materias/{idMateria}/inscripciones")
    public ResponseEntity<List<Inscripcion>> alumnosPorMateria(@PathVariable Long idMateria) {
        return ResponseEntity.ok(inscripcionService.inscripcionesPorMateriaActivas(idMateria));
    }

    /**
     * GET /api/docente/inscripciones/{id}
     * Obtiene el detalle de una inscripción específica.
     */
    @Operation(summary = "Detalle de inscripción", description = "Devuelve los datos de una inscripción por ID.")
    @GetMapping("/inscripciones/{id}")
    public ResponseEntity<?> obtenerInscripcion(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(inscripcionService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * PUT /api/docente/inscripciones/{id}/nota
     * Carga o actualiza notas parciales, nota final y asistencias de un alumno.
     * No cierra la nota; solo actualiza los valores.
     */
    @Operation(
        summary = "Cargar / actualizar nota",
        description = "El docente carga o modifica notas parciales, nota final y asistencias. "
                    + "La inscripción no debe estar CANCELADA ni con NOTA_CERRADA."
    )
    @PutMapping("/inscripciones/{id}/nota")
    public ResponseEntity<?> cargarNota(
            @PathVariable Long id,
            @Valid @RequestBody NotaRequest request) {
        try {
            Inscripcion inscripcion = inscripcionService.cargarNota(id, request);
            return ResponseEntity.ok(inscripcion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/docente/inscripciones/{id}/nota  (alias conveniente para clientes REST)
     * Mismo comportamiento que PUT pero usando POST.
     */
    @Operation(
        summary = "Cargar nota (POST)",
        description = "Alias POST del endpoint de carga de nota. Útil para clientes que no soportan PUT."
    )
    @PostMapping("/inscripciones/{id}/nota")
    public ResponseEntity<?> cargarNotaPost(
            @PathVariable Long id,
            @Valid @RequestBody NotaRequest request) {
        try {
            Inscripcion inscripcion = inscripcionService.cargarNota(id, request);
            return ResponseEntity.ok(inscripcion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * PATCH /api/docente/inscripciones/{id}/cerrar
     * Cierra la nota de la inscripción.
     */
    @Operation(
        summary = "Cerrar nota",
        description = "Cierra definitivamente la nota de la inscripción. "
                    + "notaFinal >= 6 → APROBADA | notaFinal < 6 → DESAPROBADA."
    )
    @PatchMapping("/inscripciones/{id}/cerrar")
    public ResponseEntity<?> cerrarNota(@PathVariable Long id) {
        try {
            Inscripcion inscripcion = inscripcionService.cerrarNota(id);
            return ResponseEntity.ok(inscripcion);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/docente/estudiantes/{idEstudiante}/asistencias
     * Historial de asistencias de un alumno.
     * Accesible por DOCENTE y ADMINISTRADOR.
     */
    @Operation(
        summary = "Historial de asistencias de un alumno",
        description = "Devuelve todas las inscripciones activas/con nota del estudiante indicado, "
                    + "incluyendo asistencias, parciales y nota final."
    )
    @GetMapping("/estudiantes/{idEstudiante}/asistencias")
    @PreAuthorize("hasAnyAuthority('DOCENTE','ADMINISTRADOR')")
    public ResponseEntity<?> historialAsistencias(@PathVariable Long idEstudiante) {
        try {
            List<Inscripcion> porEstudiante = inscripcionService.misInscripciones(idEstudiante)
                .stream()
                .filter(i -> !"CANCELADA".equals(i.getEstado()))
                .toList();
            return ResponseEntity.ok(porEstudiante);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
