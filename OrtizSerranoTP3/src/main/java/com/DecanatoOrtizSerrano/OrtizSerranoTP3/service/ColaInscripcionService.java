package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.ColaInscripcionResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.ColaInscripcion;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.ColaInscripcion.Estado;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Estudiante;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Inscripcion;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Materia;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.ColaInscripcionRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.EstudianteRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.InscripcionRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.MateriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ─── Capa 3: Servicio de Cola Virtual FIFO ─────────────────────────────────
 *
 * Gestiona la cola de espera por cupos en una materia.
 *
 * Flujo de uso:
 *   1. POST /api/inscripciones                → InscripcionService.inscribir()
 *      └── si "No hay cupos" → el controller llama unirseACola()
 *   2. DELETE /api/inscripciones/{id}         → InscripcionService.cancelar()
 *      └── al terminar, llama promoverSiguiente() [nueva tx]
 *   3. @Scheduled cada 60s                   → expirarVencidos()
 *
 * Configuración (application.properties):
 *   cola.expiracion.horas=48    # cuánto dura un turno en la cola
 */
@Service
public class ColaInscripcionService {

    private static final Logger log = LoggerFactory.getLogger(ColaInscripcionService.class);

    @Value("${cola.expiracion.horas:48}")
    private int expiracionHoras;

    @Autowired private ColaInscripcionRepository colaRepository;
    @Autowired private EstudianteRepository        estudianteRepository;
    @Autowired private MateriaRepository           materiaRepository;
    @Autowired private InscripcionRepository       inscripcionRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Operaciones del estudiante
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Agrega al estudiante al final de la cola FIFO de la materia.
     * Retorna la posición asignada y cuántas personas están delante.
     *
     * @throws RuntimeException si ya tiene un turno activo en esa materia
     */
    @Transactional
    public ColaInscripcionResponse unirseACola(Long idMateria, Long idEstudiante) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
            .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        Materia materia = materiaRepository.findById(idMateria)
            .orElseThrow(() -> new RuntimeException("Materia no encontrada con ID: " + idMateria));

        // Validar: ya inscripto activamente
        boolean yaInscripto = inscripcionRepository
            .existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstadoNot(
                idEstudiante, idMateria, "CANCELADA");
        if (yaInscripto) {
            throw new RuntimeException("Ya estás inscripto en esta materia");
        }

        // Validar: ya en cola (PENDIENTE)
        boolean enCola = colaRepository.existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstado(
            idEstudiante, idMateria, Estado.PENDIENTE);
        if (enCola) {
            throw new RuntimeException("Ya tenés un turno activo en la cola de esta materia");
        }

        // Calcular siguiente posición
        Integer maxPos = colaRepository.findMaxPosicion(idMateria);
        int siguientePos = (maxPos == null ? 0 : maxPos) + 1;

        // Crear el turno
        ColaInscripcion turno = new ColaInscripcion();
        turno.setEstudiante(estudiante);
        turno.setMateria(materia);
        turno.setPosicion(siguientePos);
        turno.setEstado(Estado.PENDIENTE);
        turno.setFechaSolicitud(LocalDateTime.now());
        turno.setFechaExpiracion(LocalDateTime.now().plusHours(expiracionHoras));
        colaRepository.save(turno);

        int delante = colaRepository.countDelante(idMateria, siguientePos);
        log.info("Cola: estudiante {} se sumó a cola de materia {} en posición {} ({} adelante)",
            idEstudiante, idMateria, siguientePos, delante);

        return ColaInscripcionResponse.from(turno, delante,
            "Te agregamos a la lista de espera. Hay " + delante
            + " personas delante tuyo. El turno expira en " + expiracionHoras + "h.");
    }

    /**
     * El estudiante abandona voluntariamente su turno en la cola.
     *
     * @throws RuntimeException si el turno no le pertenece o no está PENDIENTE
     */
    @Transactional
    public void abandonarCola(Long idCola, Long idEstudiante) {
        ColaInscripcion turno = colaRepository.findById(idCola)
            .orElseThrow(() -> new RuntimeException("Turno en cola no encontrado"));

        if (!turno.getEstudiante().getIdUsuario().equals(idEstudiante)) {
            throw new RuntimeException("No podés cancelar un turno que no es tuyo");
        }
        if (turno.getEstado() != Estado.PENDIENTE) {
            throw new RuntimeException("Solo podés cancelar un turno en estado PENDIENTE");
        }

        turno.setEstado(Estado.CANCELADO);
        turno.setFechaResolucion(LocalDateTime.now());
        colaRepository.save(turno);
        log.info("Cola: estudiante {} abandonó turno {} de materia {}",
            idEstudiante, idCola, turno.getMateria().getIdMateria());
    }

    /**
     * Retorna todos los turnos activos del estudiante (solo PENDIENTES).
     */
    public List<ColaInscripcionResponse> misColas(Long idEstudiante) {
        return colaRepository.findByEstudianteId(idEstudiante).stream()
            .filter(t -> t.getEstado() == Estado.PENDIENTE)
            .map(t -> {
                int delante = colaRepository.countDelante(
                    t.getMateria().getIdMateria(), t.getPosicion());
                return ColaInscripcionResponse.from(t, delante,
                    delante == 0
                        ? "¡Sos el primero en la lista!"
                        : "Hay " + delante + " persona(s) delante tuyo.");
            })
            .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Operaciones admin
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lista completa de la cola PENDIENTE de una materia (para admin).
     */
    public List<ColaInscripcionResponse> colaDeMat(Long idMateria) {
        return colaRepository.findPendientesPorMateriaOrdenados(idMateria).stream()
            .map(t -> {
                int delante = colaRepository.countDelante(idMateria, t.getPosicion());
                return ColaInscripcionResponse.from(t, delante, null);
            })
            .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Promoción automática (llamada por InscripcionService al cancelar)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Promueve al primer PENDIENTE de la cola de una materia a inscripción activa.
     *
     * REQUIRES_NEW: corre en su PROPIA transacción para que, si falla,
     * no revierta la cancelación que la disparó.
     *
     * @return Optional con la inscripción creada, o empty si la cola estaba vacía
     *         o la materia sigue sin cupos.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Inscripcion> promoverSiguiente(Long idMateria) {
        // Verificar si realmente hay un cupo libre ahora
        Materia materia = materiaRepository.findById(idMateria).orElse(null);
        if (materia == null) return Optional.empty();

        Integer maxCupos = materia.getCuposMaximos();
        if (maxCupos == null || maxCupos <= 0) return Optional.empty();

        long ocupados = inscripcionRepository.countCuposOcupados(idMateria);
        if (ocupados >= maxCupos) {
            log.debug("Cola: no hay cupo libre para materia {} tras cancelación", idMateria);
            return Optional.empty();
        }

        // Tomar el primero PENDIENTE (FIFO)
        Optional<ColaInscripcion> opt = colaRepository.findPrimeroPendiente(idMateria);
        if (opt.isEmpty()) {
            log.debug("Cola: lista de espera vacía para materia {}", idMateria);
            return Optional.empty();
        }

        ColaInscripcion turno = opt.get();

        // Crear la inscripción
        Inscripcion nueva = new Inscripcion(turno.getEstudiante(), materia, LocalDate.now());
        inscripcionRepository.save(nueva);

        // Marcar el turno como PROMOVIDO
        turno.setEstado(Estado.PROMOVIDO);
        turno.setFechaResolucion(LocalDateTime.now());
        colaRepository.save(turno);

        log.info("Cola: estudiante {} PROMOVIDO en materia {} (turno {})",
            turno.getEstudiante().getIdUsuario(), idMateria, turno.getIdCola());

        return Optional.of(nueva);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tarea programada: limpieza de colas vencidas
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cada 60 segundos marca como EXPIRADO los turnos cuya fechaExpiracion ya pasó.
     * Requiere @EnableScheduling en la clase principal.
     */
    @Scheduled(fixedDelayString = "${cola.limpieza.delay-ms:60000}")
    @Transactional
    public void expirarVencidos() {
        int expirados = colaRepository.expirarVencidos(LocalDateTime.now());
        if (expirados > 0) {
            log.info("Cola scheduler: {} turno(s) expirado(s)", expirados);
        }
    }
}
