package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.EstudianteRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Estudiante;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.EstudianteService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de Estudiantes - Solo ADMINISTRADOR
 */
@Tag(name = "Admin – Estudiantes", description = "Gestión de estudiantes (requiere rol ADMINISTRADOR)")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/estudiantes")
public class EstudianteController {

    @Autowired
    private EstudianteService estudianteService;

    /** GET /api/admin/estudiantes → Todos */
    @Operation(summary = "Listar estudiantes")
    @GetMapping
    public ResponseEntity<List<Estudiante>> listar() {
        return ResponseEntity.ok(estudianteService.listarTodos());
    }

    /** GET /api/admin/estudiantes/{id} */
    @Operation(summary = "Obtener estudiante por ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(estudianteService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse(e.getMessage()));
        }
    }

    /** POST /api/admin/estudiantes → Crear */
    @Operation(summary = "Crear estudiante", description = "Registra un nuevo estudiante con legajo, carrera y año de ingreso.")
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody EstudianteRequest request) {
        try {
            Estudiante estudiante = estudianteService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(estudiante);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PUT /api/admin/estudiantes/{id} → Actualizar */
    @Operation(summary = "Actualizar estudiante")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @Valid @RequestBody EstudianteRequest request) {
        try {
            return ResponseEntity.ok(estudianteService.actualizar(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** DELETE /api/admin/estudiantes/{id} → Eliminar */
    @Operation(summary = "Eliminar estudiante")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            estudianteService.eliminar(id);
            return ResponseEntity.ok(new MessageResponse("Estudiante eliminado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PATCH /api/admin/estudiantes/{id}/toggle → Activar/Desactivar */
    @Operation(summary = "Activar/Desactivar estudiante", description = "Alterna el estado activo del estudiante.")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(estudianteService.toggleActivo(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
