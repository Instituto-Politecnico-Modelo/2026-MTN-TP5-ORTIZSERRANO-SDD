package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.ReservaAuditorioRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Auditorio;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.ReservaAuditorio;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Usuario;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.AuditorioRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.ReservaAuditorioRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservaAuditorioService {

    @Autowired
    private ReservaAuditorioRepository reservaRepository;

    @Autowired
    private AuditorioRepository auditorioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /** Listar todas las reservas */
    public List<ReservaAuditorio> listarTodas() {
        return reservaRepository.findAll();
    }

    /** Listar reservas de un auditorio específico */
    public List<ReservaAuditorio> listarPorAuditorio(Long idAuditorio) {
        return reservaRepository.findByAuditorioIdAuditorio(idAuditorio);
    }

    /** Listar reservas de un usuario */
    public List<ReservaAuditorio> listarPorUsuario(Long idUsuario) {
        return reservaRepository.findBySolicitanteIdUsuario(idUsuario);
    }

    /** Listar reservas por estado */
    public List<ReservaAuditorio> listarPorEstado(String estado) {
        return reservaRepository.findByEstado(estado.toUpperCase());
    }

    /** Listar reservas por fecha */
    public List<ReservaAuditorio> listarPorFecha(LocalDate fecha) {
        return reservaRepository.findByFecha(fecha);
    }

    /** Listar reservas en un rango de fechas */
    public List<ReservaAuditorio> listarPorRango(LocalDate desde, LocalDate hasta) {
        return reservaRepository.findByFechaBetween(desde, hasta);
    }

    /** Obtener reserva por ID */
    public ReservaAuditorio obtenerPorId(Long id) {
        return reservaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
    }

    /** Crear reserva – valida disponibilidad y solapamientos */
    public ReservaAuditorio crear(ReservaAuditorioRequest request, Long idSolicitante) {
        Auditorio auditorio = auditorioRepository.findById(request.getIdAuditorio())
            .orElseThrow(() -> new RuntimeException("Auditorio no encontrado"));

        if (!auditorio.getActivo()) {
            throw new RuntimeException("El auditorio no está disponible");
        }

        if (request.getHoraInicio().isAfter(request.getHoraFin()) ||
            request.getHoraInicio().equals(request.getHoraFin())) {
            throw new RuntimeException("La hora de inicio debe ser anterior a la hora de fin");
        }

        if (reservaRepository.existeSolapamiento(
                auditorio.getIdAuditorio(), request.getFecha(),
                request.getHoraInicio(), request.getHoraFin())) {
            throw new RuntimeException("El auditorio ya tiene una reserva en ese horario");
        }

        Usuario solicitante = usuarioRepository.findById(idSolicitante)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        ReservaAuditorio reserva = new ReservaAuditorio();
        reserva.setAuditorio(auditorio);
        reserva.setSolicitante(solicitante);
        reserva.setMotivo(request.getMotivo());
        reserva.setDescripcion(request.getDescripcion());
        reserva.setFecha(request.getFecha());
        reserva.setHoraInicio(request.getHoraInicio());
        reserva.setHoraFin(request.getHoraFin());
        reserva.setObservaciones(request.getObservaciones());
        reserva.setEstado("PENDIENTE");

        return reservaRepository.save(reserva);
    }

    /** Cambiar estado de una reserva (solo admin) */
    public ReservaAuditorio cambiarEstado(Long id, String nuevoEstado) {
        ReservaAuditorio reserva = obtenerPorId(id);
        List<String> estadosValidos = List.of("PENDIENTE", "APROBADA", "RECHAZADA", "CANCELADA");
        if (!estadosValidos.contains(nuevoEstado.toUpperCase())) {
            throw new RuntimeException("Estado inválido. Valores permitidos: " + estadosValidos);
        }
        reserva.setEstado(nuevoEstado.toUpperCase());
        return reservaRepository.save(reserva);
    }

    /** Cancelar reserva propia */
    public ReservaAuditorio cancelar(Long id, Long idSolicitante) {
        ReservaAuditorio reserva = obtenerPorId(id);
        if (!reserva.getSolicitante().getIdUsuario().equals(idSolicitante)) {
            throw new RuntimeException("No podés cancelar una reserva de otro usuario");
        }
        if ("CANCELADA".equals(reserva.getEstado())) {
            throw new RuntimeException("La reserva ya está cancelada");
        }
        reserva.setEstado("CANCELADA");
        return reservaRepository.save(reserva);
    }

    /** Eliminar reserva (solo admin) */
    public void eliminar(Long id) {
        ReservaAuditorio reserva = obtenerPorId(id);
        reservaRepository.delete(reserva);
    }
}
