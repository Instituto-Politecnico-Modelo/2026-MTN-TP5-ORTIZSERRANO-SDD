package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.ReservaAuditorioRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Auditorio;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.ReservaAuditorio;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.security.UserDetailsImpl;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.AuditorioService;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.ReservaAuditorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas públicas (autenticadas) de auditorios y reservas.
 * Cualquier usuario autenticado puede consultar disponibilidad y hacer/cancelar sus reservas.
 */
@Tag(name = "Auditorio – Consultas", description = "Consulta de auditorios y gestión de reservas propias")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
@RestController
@RequestMapping("/api/auditorio")
public class AuditorioController {

    @Autowired
    private AuditorioService auditorioService;

    @Autowired
    private ReservaAuditorioService reservaService;

    // ── CONSULTA DE AUDITORIOS ────────────────────────────────────────────────

    /**
     * GET /api/auditorio → Listar todos los auditorios activos
     */
    @Operation(summary = "Listar auditorios disponibles",
               description = "Devuelve todos los auditorios activos del Decanato.")
    @GetMapping
    public ResponseEntity<List<Auditorio>> listarActivos() {
        return ResponseEntity.ok(auditorioService.listarActivos());
    }

    /**
     * GET /api/auditorio/{id} → Detalle de un auditorio
     */
    @Operation(summary = "Detalle de auditorio")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(auditorioService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/auditorio/capacidad?min=50 → Auditorios con capacidad mínima
     */
    @Operation(summary = "Buscar por capacidad mínima",
               description = "Filtra auditorios cuya capacidad sea igual o mayor al valor indicado.")
    @GetMapping("/capacidad")
    public ResponseEntity<List<Auditorio>> listarPorCapacidad(
            @Parameter(description = "Capacidad mínima requerida", example = "50")
            @RequestParam(defaultValue = "1") Integer min) {
        return ResponseEntity.ok(auditorioService.listarPorCapacidadMinima(min));
    }

    /**
     * GET /api/auditorio/{id}/disponibilidad?fecha=2026-05-10 → Reservas del día
     */
    @Operation(summary = "Consultar disponibilidad",
               description = "Devuelve las reservas existentes de un auditorio en una fecha dada.")
    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<?> disponibilidad(
            @PathVariable Long id,
            @Parameter(description = "Fecha a consultar (yyyy-MM-dd)", example = "2026-05-10")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            auditorioService.obtenerPorId(id); // valida que exista
            return ResponseEntity.ok(reservaService.listarPorAuditorio(id)
                .stream()
                .filter(r -> r.getFecha().equals(fecha)
                          && !r.getEstado().equals("CANCELADA")
                          && !r.getEstado().equals("RECHAZADA"))
                .toList());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }

    // ── MIS RESERVAS ──────────────────────────────────────────────────────────

    /**
     * GET /api/auditorio/mis-reservas → Reservas del usuario autenticado
     */
    @Operation(summary = "Mis reservas", description = "Lista todas las reservas del usuario que hace la consulta.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mis-reservas")
    public ResponseEntity<?> misReservas(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(reservaService.listarPorUsuario(userDetails.getId()));
    }

    /**
     * POST /api/auditorio/reservar → Crear una nueva reserva
     */
    @Operation(summary = "Reservar auditorio",
               description = "Crea una solicitud de reserva. Queda en estado PENDIENTE hasta que el admin la apruebe.")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/reservar")
    public ResponseEntity<?> reservar(@Valid @RequestBody ReservaAuditorioRequest request,
                                        Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            ReservaAuditorio reserva = reservaService.crear(request, userDetails.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(reserva);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * PATCH /api/auditorio/reservas/{id}/cancelar → Cancelar reserva propia
     */
    @Operation(summary = "Cancelar mi reserva", description = "El usuario puede cancelar únicamente sus propias reservas.")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/reservas/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id, Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return ResponseEntity.ok(reservaService.cancelar(id, userDetails.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}