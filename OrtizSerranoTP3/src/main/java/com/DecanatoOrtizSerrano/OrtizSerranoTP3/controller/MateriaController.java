package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MateriaRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Materia;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.MateriaService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de Materias - Solo ADMINISTRADOR
 * Todas las rutas están protegidas en SecurityConfig
 */
@Tag(name = "Admin – Materias", description = "Gestión de materias (requiere rol ADMINISTRADOR)")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
@RestController
@RequestMapping("/api/admin/materias")
public class MateriaController {

    @Autowired
    private MateriaService materiaService;

    /** GET /api/admin/materias → Listar todas */
    @Operation(summary = "Listar materias")
    @GetMapping
    public ResponseEntity<List<Materia>> listar() {
        return ResponseEntity.ok(materiaService.listarTodas());
    }

    /** GET /api/admin/materias/{id} → Obtener por ID */
    @Operation(summary = "Obtener materia por ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(materiaService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse(e.getMessage()));
        }
    }

    /** POST /api/admin/materias → Crear */
    @Operation(summary = "Crear materia")
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody MateriaRequest request) {
        try {
            Materia materia = materiaService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(materia);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PUT /api/admin/materias/{id} → Actualizar */
    @Operation(summary = "Actualizar materia")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @Valid @RequestBody MateriaRequest request) {
        try {
            return ResponseEntity.ok(materiaService.actualizar(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** DELETE /api/admin/materias/{id} → Eliminar */
    @Operation(summary = "Eliminar materia")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            materiaService.eliminar(id);
            return ResponseEntity.ok(new MessageResponse("Materia eliminada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
