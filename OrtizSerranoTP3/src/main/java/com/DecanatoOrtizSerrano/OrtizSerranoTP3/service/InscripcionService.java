package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.InscripcionRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.NotaRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Estudiante;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Inscripcion;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Materia;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.EstudianteRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.InscripcionRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.MateriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class InscripcionService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    /** Listar todas las inscripciones del estudiante autenticado */
    public List<Inscripcion> misInscripciones(Long idEstudiante) {
        return inscripcionRepository.findByEstudianteIdUsuario(idEstudiante);
    }

    /** Listar todas las inscripciones de una materia (admin) */
    public List<Inscripcion> inscripcionesPorMateria(Long idMateria) {
        return inscripcionRepository.findByMateriaIdMateria(idMateria);
    }

    /**
     * POST /inscripciones – Inscribir al estudiante autenticado en una materia.
     * Usa @Transactional para que Hibernate dispare el optimistic locking al hacer
     * flush: si dos hilos leen la misma versión de Materia y ambos intentan guardar,
     * el segundo recibe OptimisticLockException → evita sobrecupos.
     */
    @Transactional
    public Inscripcion inscribir(InscripcionRequest request, Long idEstudiante) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
            .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        // Se carga la Materia dentro de la transacción → Hibernate trackea su versión
        Materia materia = materiaRepository.findById(request.getIdMateria())
            .orElseThrow(() -> new RuntimeException("Materia no encontrada con ID: " + request.getIdMateria()));

        // Verificar inscripción activa previa
        boolean yaInscripto = inscripcionRepository
            .existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstadoNot(
                idEstudiante, materia.getIdMateria(), "CANCELADA");

        if (yaInscripto) {
            throw new RuntimeException("Ya estás inscripto en la materia: " + materia.getNombre());
        }

        // Verificar cupos disponibles mediante COUNT directo (índice idx_inscripciones_mat_estado)
        // → evita cargar la colección completa de inscripciones en memoria
        Integer maxCupos = materia.getCuposMaximos();
        if (maxCupos != null && maxCupos > 0) {
            long ocupados = inscripcionRepository.countCuposOcupados(materia.getIdMateria());
            if (ocupados >= maxCupos) {
                throw new RuntimeException(
                    "No hay cupos disponibles en la materia: " + materia.getNombre()
                    + " (máximo: " + maxCupos + ")");
            }
        }

        Inscripcion inscripcion = new Inscripcion(estudiante, materia, LocalDate.now());
        // Al hacer save(), Hibernate también actualiza la versión de Materia si fue tocada.
        // Si otro hilo ya actualizó la Materia en la misma transacción, se lanza OptimisticLockException.
        return inscripcionRepository.save(inscripcion);
    }

    /** Cancelar inscripción propia */
    public Inscripcion cancelar(Long idInscripcion, Long idEstudiante) {
        Inscripcion inscripcion = inscripcionRepository.findById(idInscripcion)
            .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));

        if (!inscripcion.getEstudiante().getIdUsuario().equals(idEstudiante)) {
            throw new RuntimeException("No podés cancelar una inscripción que no es tuya");
        }
        if ("CANCELADA".equals(inscripcion.getEstado())) {
            throw new RuntimeException("La inscripción ya está cancelada");
        }

        inscripcion.setEstado("CANCELADA");
        return inscripcionRepository.save(inscripcion);
    }

    /** Listar todas las inscripciones (admin) */
    public List<Inscripcion> listarTodas() {
        return inscripcionRepository.findAll();
    }

    /** Obtener inscripción por ID */
    public Inscripcion obtenerPorId(Long id) {
        return inscripcionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Inscripción no encontrada con ID: " + id));
    }

    /**
     * PUT /api/docente/inscripciones/{id}/nota
     * Docente carga o actualiza las notas parciales, finales y asistencias.
     * No cambia el estado automáticamente; eso lo hace cerrarNota().
     */
    public Inscripcion cargarNota(Long idInscripcion, NotaRequest request) {
        Inscripcion inscripcion = obtenerPorId(idInscripcion);

        if ("CANCELADA".equals(inscripcion.getEstado())) {
            throw new RuntimeException("No se pueden cargar notas en una inscripción CANCELADA");
        }
        if (inscripcion.isNotaCerrada()) {
            throw new RuntimeException("La nota ya está cerrada. No se puede modificar.");
        }

        if (request.getNotaParcial1() != null) inscripcion.setNotaParcial1(request.getNotaParcial1());
        if (request.getNotaParcial2() != null) inscripcion.setNotaParcial2(request.getNotaParcial2());
        if (request.getNotaFinal() != null) inscripcion.setNotaFinal(request.getNotaFinal());
        if (request.getAsistencias() != null) inscripcion.setAsistencias(request.getAsistencias());

        return inscripcionRepository.save(inscripcion);
    }

    /**
     * PATCH /api/docente/inscripciones/{id}/cerrar
     * Cierra la nota. Si notaFinal >= 6 → APROBADA, sino → DESAPROBADA.
     * Una vez cerrada no se puede modificar.
     */
    public Inscripcion cerrarNota(Long idInscripcion) {
        Inscripcion inscripcion = obtenerPorId(idInscripcion);

        if ("CANCELADA".equals(inscripcion.getEstado())) {
            throw new RuntimeException("No se puede cerrar una inscripción CANCELADA");
        }
        if (inscripcion.isNotaCerrada()) {
            throw new RuntimeException("La nota ya estaba cerrada");
        }
        if (inscripcion.getNotaFinal() == null) {
            throw new RuntimeException("Debe cargar la nota final antes de cerrar");
        }

        inscripcion.setEstado(inscripcion.estaAprobada() ? "APROBADA" : "DESAPROBADA");
        inscripcion.setNotaCerrada(true);
        return inscripcionRepository.save(inscripcion);
    }

    /** Listar inscripciones de una materia (para docentes) */
    public List<Inscripcion> inscripcionesPorMateriaActivas(Long idMateria) {
        return inscripcionRepository.findByMateriaIdMateria(idMateria)
            .stream()
            .filter(i -> !"CANCELADA".equals(i.getEstado()))
            .toList();
    }
}
