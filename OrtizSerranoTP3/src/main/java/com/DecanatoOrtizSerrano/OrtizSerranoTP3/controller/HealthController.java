package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * GET /api/health
 * ──────────────────────────────────────────────────────────────────
 * Endpoint público (sin autenticación) que informa si el servidor
 * está operativo. Usado por el frontend en la "Sala de Espera" para
 * hacer polling y detectar cuándo el servidor volvió a estar disponible.
 *
 * Respuesta exitosa (HTTP 200):
 * {
 *   "status": "UP",
 *   "timestamp": "2026-04-27T12:34:56Z"
 * }
 *
 * Nota: si el servidor realmente no puede responder (saturado), este
 * endpoint devolverá 503 o no responderá, lo que el frontend interpreta
 * como "todavía no disponible".
 */
@Tag(name = "Health", description = "Estado del servidor")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(
        summary     = "Estado del servidor",
        description = "Retorna UP si el servidor está operativo. "
                    + "Usado por el frontend para polling desde la sala de espera."
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status",    "UP",
            "timestamp", Instant.now().toString()
        ));
    }
}
