package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Materia;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.MateriaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consulta pública de materias (cualquier usuario autenticado).
 */
@Tag(name = "Materias – Consulta", description = "Listado de materias disponibles para inscripción")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/materias")
public class MateriaPublicaController {

    @Autowired
    private MateriaRepository materiaRepository;

    /** GET /api/materias → Todas las materias */
    @Operation(summary = "Listar todas las materias")
    @GetMapping
    public ResponseEntity<List<Materia>> listar() {
        return ResponseEntity.ok(materiaRepository.findAll());
    }

    /** GET /api/materias/{id} → Detalle de una materia */
    @Operation(summary = "Obtener materia por ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return materiaRepository.findById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Materia no encontrada"));
    }

    /** GET /api/materias/anio/{anio} → Materias por año */
    @Operation(summary = "Materias por año")
    @GetMapping("/anio/{anio}")
    public ResponseEntity<List<Materia>> listarPorAnio(@PathVariable Integer anio) {
        return ResponseEntity.ok(
            materiaRepository.findAll().stream()
                .filter(m -> anio.equals(m.getAnio()))
                .toList()
        );
    }
}
