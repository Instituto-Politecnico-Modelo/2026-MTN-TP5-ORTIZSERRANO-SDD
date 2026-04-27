package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.InscripcionRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.NotaRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Estudiante;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Inscripcion;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Materia;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.EstudianteRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.InscripcionRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.MateriaRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.InscripcionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de inscripciones.
 *
 * Cubre:
 *  1. Inscripción exitosa (sin cupos limitados)
 *  2. Inscripción exitosa (con cupos disponibles)
 *  3. Error: estudiante no existe
 *  4. Error: materia no existe
 *  5. Error: ya inscripto (estado != CANCELADA)
 *  6. Error: cupos agotados
 *  7. Cancelación exitosa
 *  8. Error: cancelar inscripción ajena
 *  9. Error: cancelar ya cancelada
 * 10. Cargar nota exitoso
 * 11. Error: cargar nota en inscripción CANCELADA
 * 12. Error: cargar nota con nota cerrada
 * 13. Cerrar nota → estado APROBADA (nota >= 6)
 * 14. Cerrar nota → estado DESAPROBADA (nota < 6)
 * 15. Error: cerrar nota sin nota final
 * 16. Error: cerrar nota ya cerrada
 * 17. Error: cerrar nota CANCELADA
 * 18. misInscripciones delega en repositorio
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InscripcionService – Pruebas unitarias")
class InscripcionServiceTest {

    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private EstudianteRepository  estudianteRepository;
    @Mock private MateriaRepository     materiaRepository;

    @InjectMocks
    private InscripcionService inscripcionService;

    // ── Fixtures ──────────────────────────────────────────────────────────────
    private Estudiante estudiante;
    private Materia    materia;
    private Inscripcion inscripcion;

    @BeforeEach
    void setUp() {
        estudiante = new Estudiante();
        estudiante.setIdUsuario(10L);
        estudiante.setNombre("Ana");
        estudiante.setApellido("García");
        estudiante.setEmail("ana@test.com");

        materia = new Materia("MAT01", "Matemáticas", null, 3);
        materia.setIdMateria(20L);
        // sin límite de cupos por defecto

        inscripcion = new Inscripcion(estudiante, materia, LocalDate.now());
        inscripcion.setIdInscripcion(100L);
    }

    // ─── 1. Inscripción exitosa (sin límite de cupos) ─────────────────────────

    @Test
    @DisplayName("inscribir() crea inscripción ACTIVA cuando no hay límite de cupos")
    void inscribir_sinLimiteCupos_creaActiva() {
        InscripcionRequest req = new InscripcionRequest();
        req.setIdMateria(20L);

        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        when(materiaRepository.findById(20L)).thenReturn(Optional.of(materia));
        when(inscripcionRepository.existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstadoNot(
                10L, 20L, "CANCELADA")).thenReturn(false);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(i -> i.getArgument(0));

        Inscripcion resultado = inscripcionService.inscribir(req, 10L);

        assertEquals("ACTIVA", resultado.getEstado());
        assertEquals(estudiante, resultado.getEstudiante());
        assertEquals(materia, resultado.getMateria());
        verify(inscripcionRepository).save(any(Inscripcion.class));
    }

    // ─── 2. Inscripción exitosa (con cupos disponibles) ──────────────────────

    @Test
    @DisplayName("inscribir() con cuposMaximos=5 y solo 3 ocupados → OK")
    void inscribir_conCuposDisponibles_creaActiva() {
        materia.setCuposMaximos(5);
        InscripcionRequest req = new InscripcionRequest();
        req.setIdMateria(20L);

        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        when(materiaRepository.findById(20L)).thenReturn(Optional.of(materia));
        when(inscripcionRepository.existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstadoNot(
                10L, 20L, "CANCELADA")).thenReturn(false);
        when(inscripcionRepository.countCuposOcupados(20L)).thenReturn(3L);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(i -> i.getArgument(0));

        Inscripcion resultado = inscripcionService.inscribir(req, 10L);

        assertEquals("ACTIVA", resultado.getEstado());
    }

    // ─── 3. Estudiante no existe ──────────────────────────────────────────────

    @Test
    @DisplayName("inscribir() lanza excepción si el estudiante no existe")
    void inscribir_estudianteNoExiste_lanzaExcepcion() {
        InscripcionRequest req = new InscripcionRequest();
        req.setIdMateria(20L);

        when(estudianteRepository.findById(10L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.inscribir(req, 10L));
        assertEquals("Estudiante no encontrado", ex.getMessage());
        verify(materiaRepository, never()).findById(any());
    }

    // ─── 4. Materia no existe ─────────────────────────────────────────────────

    @Test
    @DisplayName("inscribir() lanza excepción si la materia no existe")
    void inscribir_materiaNoExiste_lanzaExcepcion() {
        InscripcionRequest req = new InscripcionRequest();
        req.setIdMateria(99L);

        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.inscribir(req, 10L));
        assertTrue(ex.getMessage().contains("Materia no encontrada con ID: 99"));
    }

    // ─── 5. Ya inscripto ──────────────────────────────────────────────────────

    @Test
    @DisplayName("inscribir() lanza excepción si el estudiante ya está inscripto")
    void inscribir_yaInscripto_lanzaExcepcion() {
        InscripcionRequest req = new InscripcionRequest();
        req.setIdMateria(20L);

        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        when(materiaRepository.findById(20L)).thenReturn(Optional.of(materia));
        when(inscripcionRepository.existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstadoNot(
                10L, 20L, "CANCELADA")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.inscribir(req, 10L));
        assertTrue(ex.getMessage().contains("Ya estás inscripto en la materia:"));
        verify(inscripcionRepository, never()).save(any());
    }

    // ─── 6. Cupos agotados ────────────────────────────────────────────────────

    @Test
    @DisplayName("inscribir() lanza excepción cuando cupos están agotados")
    void inscribir_cuposAgotados_lanzaExcepcion() {
        materia.setCuposMaximos(2);
        InscripcionRequest req = new InscripcionRequest();
        req.setIdMateria(20L);

        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante));
        when(materiaRepository.findById(20L)).thenReturn(Optional.of(materia));
        when(inscripcionRepository.existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstadoNot(
                10L, 20L, "CANCELADA")).thenReturn(false);
        when(inscripcionRepository.countCuposOcupados(20L)).thenReturn(2L);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.inscribir(req, 10L));
        assertTrue(ex.getMessage().contains("No hay cupos disponibles en la materia:"));
        assertTrue(ex.getMessage().contains("máximo: 2"));
    }

    // ─── 7. Cancelación exitosa ───────────────────────────────────────────────

    @Test
    @DisplayName("cancelar() cambia estado a CANCELADA correctamente")
    void cancelar_exitoso_cambiaEstado() {
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(i -> i.getArgument(0));

        Inscripcion resultado = inscripcionService.cancelar(100L, 10L);

        assertEquals("CANCELADA", resultado.getEstado());
        verify(inscripcionRepository).save(inscripcion);
    }

    // ─── 8. Cancelar inscripción ajena ────────────────────────────────────────

    @Test
    @DisplayName("cancelar() lanza excepción si la inscripción no pertenece al estudiante")
    void cancelar_inscripcionAjena_lanzaExcepcion() {
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));

        // idEstudiante=999 no coincide con inscripcion.estudiante.idUsuario=10
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.cancelar(100L, 999L));
        assertEquals("No podés cancelar una inscripción que no es tuya", ex.getMessage());
    }

    // ─── 9. Cancelar ya cancelada ─────────────────────────────────────────────

    @Test
    @DisplayName("cancelar() lanza excepción si ya está cancelada")
    void cancelar_yaCancelada_lanzaExcepcion() {
        inscripcion.setEstado("CANCELADA");
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.cancelar(100L, 10L));
        assertEquals("La inscripción ya está cancelada", ex.getMessage());
    }

    // ─── 10. Cargar nota exitoso ──────────────────────────────────────────────

    @Test
    @DisplayName("cargarNota() actualiza parciales, final y asistencias")
    void cargarNota_exitoso_actualizaCampos() {
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(i -> i.getArgument(0));

        NotaRequest req = new NotaRequest();
        req.setNotaParcial1(7.0);
        req.setNotaParcial2(8.0);
        req.setNotaFinal(7.5);
        req.setAsistencias(85);

        Inscripcion resultado = inscripcionService.cargarNota(100L, req);

        assertEquals(7.0, resultado.getNotaParcial1());
        assertEquals(8.0, resultado.getNotaParcial2());
        assertEquals(7.5, resultado.getNotaFinal());
        assertEquals(85,  resultado.getAsistencias());
    }

    // ─── 11. Cargar nota en CANCELADA ────────────────────────────────────────

    @Test
    @DisplayName("cargarNota() lanza excepción si la inscripción está CANCELADA")
    void cargarNota_cancelada_lanzaExcepcion() {
        inscripcion.setEstado("CANCELADA");
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));

        NotaRequest req = new NotaRequest();
        req.setNotaFinal(8.0);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.cargarNota(100L, req));
        assertEquals("No se pueden cargar notas en una inscripción CANCELADA", ex.getMessage());
    }

    // ─── 12. Cargar nota con nota cerrada ─────────────────────────────────────

    @Test
    @DisplayName("cargarNota() lanza excepción si la nota ya fue cerrada")
    void cargarNota_notaCerrada_lanzaExcepcion() {
        inscripcion.setNotaCerrada(true);
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));

        NotaRequest req = new NotaRequest();
        req.setNotaFinal(9.0);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.cargarNota(100L, req));
        assertEquals("La nota ya está cerrada. No se puede modificar.", ex.getMessage());
    }

    // ─── 13. Cerrar nota → APROBADA ───────────────────────────────────────────

    @Test
    @DisplayName("cerrarNota() con notaFinal=7 → estado APROBADA y notaCerrada=true")
    void cerrarNota_notaAprobatoria_estadoAprobada() {
        inscripcion.setNotaFinal(7.0);
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(i -> i.getArgument(0));

        Inscripcion resultado = inscripcionService.cerrarNota(100L);

        assertEquals("APROBADA", resultado.getEstado());
        assertTrue(resultado.isNotaCerrada());
    }

    // ─── 14. Cerrar nota → DESAPROBADA ────────────────────────────────────────

    @Test
    @DisplayName("cerrarNota() con notaFinal=4 → estado DESAPROBADA y notaCerrada=true")
    void cerrarNota_notaReprobatoria_estadoDesaprobada() {
        inscripcion.setNotaFinal(4.0);
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(i -> i.getArgument(0));

        Inscripcion resultado = inscripcionService.cerrarNota(100L);

        assertEquals("DESAPROBADA", resultado.getEstado());
        assertTrue(resultado.isNotaCerrada());
    }

    // ─── 15. Cerrar nota sin nota final ───────────────────────────────────────

    @Test
    @DisplayName("cerrarNota() lanza excepción si no hay nota final")
    void cerrarNota_sinNotaFinal_lanzaExcepcion() {
        // notaFinal es null por defecto
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.cerrarNota(100L));
        assertEquals("Debe cargar la nota final antes de cerrar", ex.getMessage());
    }

    // ─── 16. Cerrar nota ya cerrada ───────────────────────────────────────────

    @Test
    @DisplayName("cerrarNota() lanza excepción si la nota ya estaba cerrada")
    void cerrarNota_yaCerrada_lanzaExcepcion() {
        inscripcion.setNotaFinal(8.0);
        inscripcion.setNotaCerrada(true);
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.cerrarNota(100L));
        assertEquals("La nota ya estaba cerrada", ex.getMessage());
    }

    // ─── 17. Cerrar nota CANCELADA ────────────────────────────────────────────

    @Test
    @DisplayName("cerrarNota() lanza excepción si la inscripción está CANCELADA")
    void cerrarNota_cancelada_lanzaExcepcion() {
        inscripcion.setEstado("CANCELADA");
        when(inscripcionRepository.findById(100L)).thenReturn(Optional.of(inscripcion));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> inscripcionService.cerrarNota(100L));
        assertEquals("No se puede cerrar una inscripción CANCELADA", ex.getMessage());
    }

    // ─── 18. misInscripciones delega en repositorio ───────────────────────────

    @Test
    @DisplayName("misInscripciones() retorna la lista del repositorio sin modificarla")
    void misInscripciones_delegaEnRepositorio() {
        List<Inscripcion> lista = List.of(inscripcion);
        when(inscripcionRepository.findByEstudianteIdUsuario(10L)).thenReturn(lista);

        List<Inscripcion> resultado = inscripcionService.misInscripciones(10L);

        assertEquals(1, resultado.size());
        assertEquals(inscripcion, resultado.get(0));
        verify(inscripcionRepository).findByEstudianteIdUsuario(10L);
    }
}
