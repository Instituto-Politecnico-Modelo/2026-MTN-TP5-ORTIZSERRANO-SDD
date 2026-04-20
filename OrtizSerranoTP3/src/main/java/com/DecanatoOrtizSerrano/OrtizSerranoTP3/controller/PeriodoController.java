package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.PeriodoRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Periodo;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.PeriodoService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de Períodos - Solo ADMINISTRADOR
 */
@Tag(name = "Admin – Períodos", description = "Gestión de períodos académicos (requiere rol ADMINISTRADOR)")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/periodos")
public class PeriodoController {

    @Autowired
    private PeriodoService periodoService;

    /** GET /api/admin/periodos → Todos */
    @Operation(summary = "Listar períodos", description = "Devuelve todos los períodos académicos.")
    @GetMapping
    public ResponseEntity<List<Periodo>> listar() {
        return ResponseEntity.ok(periodoService.listarTodos());
    }

    /** GET /api/admin/periodos/activos → Solo activos */
    @Operation(summary = "Listar períodos activos", description = "Devuelve solo los períodos con estado activo = true.")
    @GetMapping("/activos")
    public ResponseEntity<List<Periodo>> listarActivos() {
        return ResponseEntity.ok(periodoService.listarActivos());
    }

    /** GET /api/admin/periodos/{id} */
    @Operation(summary = "Obtener período por ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(periodoService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse(e.getMessage()));
        }
    }

    /** POST /api/admin/periodos → Crear */
    @Operation(summary = "Crear período")
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody PeriodoRequest request) {
        try {
            Periodo periodo = periodoService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(periodo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PUT /api/admin/periodos/{id} → Actualizar */
    @Operation(summary = "Actualizar período")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @Valid @RequestBody PeriodoRequest request) {
        try {
            return ResponseEntity.ok(periodoService.actualizar(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** DELETE /api/admin/periodos/{id} → Eliminar */
    @Operation(summary = "Eliminar período")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            periodoService.eliminar(id);
            return ResponseEntity.ok(new MessageResponse("Período eliminado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PATCH /api/admin/periodos/{id}/toggle → Activar/Desactivar */
    @Operation(summary = "Activar/Desactivar período", description = "Alterna el estado activo del período.")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(periodoService.toggleActivo(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
