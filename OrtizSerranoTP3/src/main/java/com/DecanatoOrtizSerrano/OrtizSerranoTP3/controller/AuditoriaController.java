package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.AuditoriaIntegridadResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.RegistroAuditoria;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints de consulta y verificación de la auditoría encadenada.
 * Solo accesible por ADMINISTRADOR.
 */
@Tag(name = "Auditoría", description = "Consulta y verificación de integridad del log de auditoría encadenada")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/auditoria")
@PreAuthorize("hasAuthority('ADMINISTRADOR')")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    /**
     * GET /api/admin/auditoria
     * Lista todos los registros de auditoría en orden cronológico.
     */
    @Operation(summary = "Listar todos los registros",
               description = "Devuelve el log completo de auditoría encadenada en orden ascendente.")
    @GetMapping
    public ResponseEntity<List<RegistroAuditoria>> listarTodos() {
        return ResponseEntity.ok(auditoriaService.listarTodos());
    }

    /**
     * GET /api/admin/auditoria/entidad/{entidad}
     * Filtra registros por tipo de entidad (ej: "Inscripcion", "Materia").
     */
    @Operation(summary = "Filtrar por entidad",
               description = "Devuelve registros de una entidad específica (ej: Inscripcion, Materia, Auditorio).")
    @GetMapping("/entidad/{entidad}")
    public ResponseEntity<List<RegistroAuditoria>> porEntidad(@PathVariable String entidad) {
        return ResponseEntity.ok(auditoriaService.porEntidad(entidad));
    }

    /**
     * GET /api/admin/auditoria/entidad/{entidad}/{id}
     * Filtra por entidad e ID del objeto (historial de un objeto específico).
     */
    @Operation(summary = "Historial de un objeto",
               description = "Devuelve todos los registros de auditoría de un objeto específico (entidad + id).")
    @GetMapping("/entidad/{entidad}/{id}")
    public ResponseEntity<List<RegistroAuditoria>> porEntidadYId(
            @PathVariable String entidad, @PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.porEntidadYId(entidad, id));
    }

    /**
     * GET /api/admin/auditoria/usuario/{idUsuario}
     * Filtra por ID de usuario — todas las acciones que realizó.
     */
    @Operation(summary = "Acciones de un usuario",
               description = "Devuelve todos los registros generados por un usuario específico.")
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<RegistroAuditoria>> porUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(auditoriaService.porUsuario(idUsuario));
    }

    /**
     * GET /api/admin/auditoria/accion/{accion}
     * Filtra por tipo de acción (CREAR, MODIFICAR, ELIMINAR, CERRAR_NOTA, etc.).
     */
    @Operation(summary = "Filtrar por acción",
               description = "Devuelve registros de un tipo de acción (CREAR, MODIFICAR, ELIMINAR, CERRAR_NOTA, etc.).")
    @GetMapping("/accion/{accion}")
    public ResponseEntity<List<RegistroAuditoria>> porAccion(@PathVariable String accion) {
        return ResponseEntity.ok(auditoriaService.porAccion(accion));
    }

    /**
     * GET /api/admin/auditoria/rango?desde=...&hasta=...
     * Filtra por rango de fechas (formato ISO: 2026-04-20T00:00:00).
     */
    @Operation(summary = "Filtrar por rango de fechas",
               description = "Devuelve registros entre dos timestamps. Formato: 2026-04-20T00:00:00")
    @GetMapping("/rango")
    public ResponseEntity<List<RegistroAuditoria>> porRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(auditoriaService.porRangoFecha(desde, hasta));
    }

    /**
     * GET /api/admin/auditoria/verificar
     * Verifica la integridad completa de la cadena recomputando cada hash.
     * Si todos coinciden → cadena íntegra.
     * Si alguno no coincide → datos manipulados.
     */
    @Operation(summary = "Verificar integridad de la cadena",
               description = "Recorre todos los registros y recomputa cada hash SHA-256. "
                           + "Devuelve lista de errores (vacía = cadena íntegra).")
    @GetMapping("/verificar")
    public ResponseEntity<AuditoriaIntegridadResponse> verificarIntegridad() {
        List<String> errores = auditoriaService.verificarIntegridad();
        int total = auditoriaService.listarTodos().size();
        boolean integra = errores.isEmpty();
        return ResponseEntity.ok(new AuditoriaIntegridadResponse(integra, total, errores));
    }
}
