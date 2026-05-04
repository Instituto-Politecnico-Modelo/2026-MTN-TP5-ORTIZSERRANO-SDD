package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MateriaDisponibleResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Materia;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.InscripcionRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.MateriaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consulta pública de materias (cualquier usuario autenticado).
 *
 * Endpoints:
 *  GET /api/materias                          → todas las materias con cupos
 *  GET /api/materias/disponibles              → solo las que tienen cupos libres
 *  GET /api/materias/{id}                     → detalle con cupos de una materia
 *  GET /api/materias/anio/{anio}              → materias de un año con cupos
 *  GET /api/materias/anio/{anio}/cuatrimestre/{c} → por año y cuatrimestre
 *  GET /api/materias/codigo/{codigo}          → buscar por código exacto
 */
@Tag(name = "Materias – Consulta", description = "Listado de materias con estado de cupos disponibles")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
@RestController
@RequestMapping("/api/materias")
public class MateriaPublicaController {

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    // ─── Mapper ───────────────────────────────────────────────────────────────

    /**
     * Convierte una Materia a su DTO con cupos calculados.
     * Usa countCuposOcupados() del repositorio (COUNT con índice, sin cargar colección).
     */
    private MateriaDisponibleResponse toDto(Materia m) {
        long ocupados = inscripcionRepository.countCuposOcupados(m.getIdMateria());

        MateriaDisponibleResponse dto = new MateriaDisponibleResponse();
        dto.setIdMateria(m.getIdMateria());
        dto.setCodigo(m.getCodigo());
        dto.setNombre(m.getNombre());
        dto.setDescripcion(m.getDescripcion());
        dto.setCreditos(m.getCreditos());
        dto.setAnio(m.getAnio());
        dto.setCuatrimestre(m.getCuatrimestre());
        dto.setCuposMaximos(m.getCuposMaximos());
        dto.setCuposOcupados((int) ocupados);

        if (m.getDocente() != null) {
            dto.setDocenteNombre(m.getDocente().getNombre());
            dto.setDocenteApellido(m.getDocente().getApellido());
            dto.setDocenteEmail(m.getDocente().getEmail());
        }

        if (m.getCuposMaximos() != null && m.getCuposMaximos() > 0) {
            int disponibles = m.getCuposMaximos() - (int) ocupados;
            dto.setCuposDisponibles(Math.max(disponibles, 0));
            dto.setHayLugar(disponibles > 0);
            int pct = (int) Math.round(ocupados * 100.0 / m.getCuposMaximos());
            dto.setPorcentajeOcupacion(Math.min(pct, 100));
        } else {
            // Sin límite de cupos
            dto.setCuposDisponibles(null);
            dto.setHayLugar(true);
            dto.setPorcentajeOcupacion(null);
        }

        return dto;
    }

    // ─── Endpoints ────────────────────────────────────────────────────────────

    /**
     * GET /api/materias
     * Lista TODAS las materias con el detalle de cupos (máximos, ocupados,
     * disponibles, porcentaje de ocupación).
     */
    @Operation(
        summary = "Listar todas las materias con cupos",
        description = "Devuelve todas las materias registradas junto con el estado actual de sus cupos: " +
                      "cuposMaximos, cuposOcupados, cuposDisponibles, hayLugar y porcentajeOcupacion."
    )
    @GetMapping
    public ResponseEntity<List<MateriaDisponibleResponse>> listar() {
        List<MateriaDisponibleResponse> result = materiaRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/materias/disponibles
     * Lista solo las materias que tienen cupos libres (hayLugar = true).
     * Incluye materias sin límite de cupos configurado.
     */
    @Operation(
        summary = "Listar materias con cupos disponibles",
        description = "Devuelve únicamente las materias en las que todavía es posible inscribirse " +
                      "(cupos no agotados). Las materias sin límite siempre aparecen."
    )
    @GetMapping("/disponibles")
    public ResponseEntity<List<MateriaDisponibleResponse>> listarDisponibles() {
        List<MateriaDisponibleResponse> result = materiaRepository.findAll()
                .stream()
                .map(this::toDto)
                .filter(MateriaDisponibleResponse::isHayLugar)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/materias/{id}
     * Detalle de una materia específica con cupos.
     */
    @Operation(
        summary = "Obtener materia por ID con cupos",
        description = "Devuelve el detalle completo de una materia incluyendo el estado actual de cupos."
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(
            @Parameter(description = "ID de la materia") @PathVariable Long id) {
        return materiaRepository.findById(id)
                .<ResponseEntity<?>>map(m -> ResponseEntity.ok(toDto(m)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Materia no encontrada"));
    }

    /**
     * GET /api/materias/codigo/{codigo}
     * Busca una materia por su código único (ej: "MAT-001").
     */
    @Operation(
        summary = "Buscar materia por código",
        description = "Devuelve la materia que tenga el código exacto indicado, con cupos actualizados."
    )
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<?> obtenerPorCodigo(
            @Parameter(description = "Código único de la materia, ej: MAT-001") @PathVariable String codigo) {
        return materiaRepository.findByCodigo(codigo)
                .<ResponseEntity<?>>map(m -> ResponseEntity.ok(toDto(m)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Materia con código '" + codigo + "' no encontrada"));
    }

    /**
     * GET /api/materias/anio/{anio}
     * Lista materias de un año académico con sus cupos.
     */
    @Operation(
        summary = "Materias por año académico",
        description = "Devuelve todas las materias del año indicado con el estado de cupos."
    )
    @GetMapping("/anio/{anio}")
    public ResponseEntity<List<MateriaDisponibleResponse>> listarPorAnio(
            @Parameter(description = "Año académico, ej: 1, 2, 3") @PathVariable Integer anio) {
        List<MateriaDisponibleResponse> result = materiaRepository.findAll().stream()
                .filter(m -> anio.equals(m.getAnio()))
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/materias/anio/{anio}/cuatrimestre/{cuatrimestre}
     * Lista materias filtradas por año Y cuatrimestre con sus cupos.
     */
    @Operation(
        summary = "Materias por año y cuatrimestre",
        description = "Devuelve las materias del año y cuatrimestre indicados con el estado de cupos."
    )
    @GetMapping("/anio/{anio}/cuatrimestre/{cuatrimestre}")
    public ResponseEntity<List<MateriaDisponibleResponse>> listarPorAnioYCuatrimestre(
            @Parameter(description = "Año académico") @PathVariable Integer anio,
            @Parameter(description = "Cuatrimestre: 1 o 2")  @PathVariable Integer cuatrimestre) {
        List<MateriaDisponibleResponse> result = materiaRepository.findAll().stream()
                .filter(m -> anio.equals(m.getAnio()) && cuatrimestre.equals(m.getCuatrimestre()))
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(result);
    }
}
