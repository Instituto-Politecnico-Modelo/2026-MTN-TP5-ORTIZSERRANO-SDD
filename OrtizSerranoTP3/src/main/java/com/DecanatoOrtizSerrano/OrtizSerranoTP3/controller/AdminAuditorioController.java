package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.AuditorioRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Auditorio;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.ReservaAuditorio;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.AuditorioService;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.ReservaAuditorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión administrativa de Auditorios y Reservas – Solo ADMINISTRADOR.
 */
@Tag(name = "Admin – Auditorios", description = "Gestión de auditorios y aprobación de reservas (requiere ADMINISTRADOR)")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/auditorios")
public class AdminAuditorioController {

    @Autowired
    private AuditorioService auditorioService;

    @Autowired
    private ReservaAuditorioService reservaService;

    // ── CRUD AUDITORIOS ───────────────────────────────────────────────────────

    @Operation(summary = "Listar todos los auditorios (incl. inactivos)")
    @GetMapping
    public ResponseEntity<List<Auditorio>> listar() {
        return ResponseEntity.ok(auditorioService.listarTodos());
    }

    @Operation(summary = "Obtener auditorio por ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(auditorioService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Crear auditorio")
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody AuditorioRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(auditorioService.crear(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar auditorio")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @Valid @RequestBody AuditorioRequest request) {
        try {
            return ResponseEntity.ok(auditorioService.actualizar(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar auditorio")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            auditorioService.eliminar(id);
            return ResponseEntity.ok(new MessageResponse("Auditorio eliminado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Activar/Desactivar auditorio")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(auditorioService.toggleActivo(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // ── GESTIÓN DE RESERVAS ───────────────────────────────────────────────────

    @Operation(summary = "Listar todas las reservas")
    @GetMapping("/reservas")
    public ResponseEntity<List<ReservaAuditorio>> listarReservas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    @Operation(summary = "Listar reservas por estado",
               description = "Estados válidos: PENDIENTE, APROBADA, RECHAZADA, CANCELADA")
    @GetMapping("/reservas/estado/{estado}")
    public ResponseEntity<List<ReservaAuditorio>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(reservaService.listarPorEstado(estado));
    }

    @Operation(summary = "Cambiar estado de una reserva",
               description = "Aprueba, rechaza o cancela una reserva. Body: { \"estado\": \"APROBADA\" }")
    @PatchMapping("/reservas/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @RequestParam String estado) {
        try {
            return ResponseEntity.ok(reservaService.cambiarEstado(id, estado));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar reserva")
    @DeleteMapping("/reservas/{id}")
    public ResponseEntity<?> eliminarReserva(@PathVariable Long id) {
        try {
            reservaService.eliminar(id);
            return ResponseEntity.ok(new MessageResponse("Reserva eliminada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
