package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.AuditorioRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Auditorio;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.AuditorioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditorioService {

    @Autowired
    private AuditorioRepository auditorioRepository;

    /** Listar todos los auditorios */
    public List<Auditorio> listarTodos() {
        return auditorioRepository.findAll();
    }

    /** Listar solo los auditorios activos */
    public List<Auditorio> listarActivos() {
        return auditorioRepository.findByActivoTrue();
    }

    /** Listar auditorios con capacidad mínima */
    public List<Auditorio> listarPorCapacidadMinima(Integer capacidadMinima) {
        return auditorioRepository.findByCapacidadGreaterThanEqual(capacidadMinima);
    }

    /** Obtener auditorio por ID */
    public Auditorio obtenerPorId(Long id) {
        return auditorioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Auditorio no encontrado con ID: " + id));
    }

    /** Crear auditorio */
    public Auditorio crear(AuditorioRequest request) {
        if (auditorioRepository.existsByNombre(request.getNombre())) {
            throw new RuntimeException("Ya existe un auditorio con el nombre: " + request.getNombre());
        }
        Auditorio auditorio = new Auditorio();
        mapearDesdeRequest(auditorio, request);
        return auditorioRepository.save(auditorio);
    }

    /** Actualizar auditorio */
    public Auditorio actualizar(Long id, AuditorioRequest request) {
        Auditorio auditorio = obtenerPorId(id);
        // Verificar nombre duplicado solo si cambió
        if (!auditorio.getNombre().equals(request.getNombre())
                && auditorioRepository.existsByNombre(request.getNombre())) {
            throw new RuntimeException("Ya existe un auditorio con el nombre: " + request.getNombre());
        }
        mapearDesdeRequest(auditorio, request);
        return auditorioRepository.save(auditorio);
    }

    /** Eliminar auditorio */
    public void eliminar(Long id) {
        Auditorio auditorio = obtenerPorId(id);
        auditorioRepository.delete(auditorio);
    }

    /** Activar / Desactivar auditorio */
    public Auditorio toggleActivo(Long id) {
        Auditorio auditorio = obtenerPorId(id);
        auditorio.setActivo(!auditorio.getActivo());
        return auditorioRepository.save(auditorio);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private void mapearDesdeRequest(Auditorio auditorio, AuditorioRequest request) {
        auditorio.setNombre(request.getNombre());
        auditorio.setDescripcion(request.getDescripcion());
        auditorio.setCapacidad(request.getCapacidad());
        auditorio.setUbicacion(request.getUbicacion());
        auditorio.setEquipamiento(request.getEquipamiento());
    }
}
