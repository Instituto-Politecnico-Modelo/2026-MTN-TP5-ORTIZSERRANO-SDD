package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.RegistroAuditoria;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.AuditoriaRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.AuditoriaService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de Auditoría Encadenada.
 *
 * Cubre:
 *  1. registrar() encadena al GENESIS cuando no hay registros previos
 *  2. registrar() encadena al hash del último registro existente
 *  3. El hashActual es determinista (mismo input → mismo hash)
 *  4. calcularHash() produce hex SHA-256 de 64 caracteres
 *  5. verificarIntegridad() retorna lista vacía para cadena íntegra
 *  6. verificarIntegridad() detecta hashAnterior roto
 *  7. verificarIntegridad() detecta datos manipulados (hashActual no coincide)
 *  8. porEntidad() delega en repositorio
 *  9. porAccion() delega en repositorio
 * 10. listarTodos() delega en repositorio
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaService – Pruebas unitarias")
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @InjectMocks
    private AuditoriaService auditoriaService;

    private static final String GENESIS = "0000000000000000000000000000000000000000000000000000000000000000";
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 4, 27, 10, 0, 0);

    // ─── 1. Primer registro usa GENESIS ──────────────────────────────────────

    @Test
    @DisplayName("registrar() usa GENESIS como hashAnterior cuando no hay registros")
    void registrar_sinRegistrosPrevios_usaGenesis() {
        when(auditoriaRepository.findUltimoRegistro()).thenReturn(Optional.empty());
        when(auditoriaRepository.save(any(RegistroAuditoria.class))).thenAnswer(i -> i.getArgument(0));

        RegistroAuditoria resultado = auditoriaService.registrar(
            "Inscripcion", 1L, "CREAR", "Inscripción creada", 10L, "ana@test.com", "127.0.0.1");

        assertEquals(GENESIS, resultado.getHashAnterior());
        assertNotNull(resultado.getHashActual());
        assertFalse(resultado.getHashActual().isEmpty());
        verify(auditoriaRepository).save(resultado);
    }

    // ─── 2. Encadena al hash del registro anterior ────────────────────────────

    @Test
    @DisplayName("registrar() usa el hashActual del último registro como hashAnterior")
    void registrar_conRegistroPrevio_encadenaHash() {
        RegistroAuditoria anterior = new RegistroAuditoria();
        anterior.setHashActual("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");

        when(auditoriaRepository.findUltimoRegistro()).thenReturn(Optional.of(anterior));
        when(auditoriaRepository.save(any(RegistroAuditoria.class))).thenAnswer(i -> i.getArgument(0));

        RegistroAuditoria resultado = auditoriaService.registrar(
            "Materia", 5L, "MODIFICAR", "Cupos actualizados", 1L, "admin@decanato.edu", null);

        assertEquals(anterior.getHashActual(), resultado.getHashAnterior());
    }

    // ─── 3. hashActual es determinista ────────────────────────────────────────

    @Test
    @DisplayName("calcularHash() produce el mismo hash para el mismo input")
    void calcularHash_mismosInputs_mismosHash() {
        String h1 = auditoriaService.calcularHash(GENESIS, "Inscripcion", 1L, "CREAR", "desc", AHORA, 10L);
        String h2 = auditoriaService.calcularHash(GENESIS, "Inscripcion", 1L, "CREAR", "desc", AHORA, 10L);

        assertEquals(h1, h2);
    }

    // ─── 4. calcularHash() produce hex SHA-256 de 64 chars ───────────────────

    @Test
    @DisplayName("calcularHash() produce un hex de 64 caracteres (SHA-256)")
    void calcularHash_produceSHA256De64Chars() {
        String hash = auditoriaService.calcularHash(GENESIS, "Test", 1L, "ACCION", "desc", AHORA, 1L);

        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 debe producir 64 caracteres hex");
        assertTrue(hash.matches("[0-9a-f]{64}"), "Debe ser hex minúsculas");
    }

    // ─── 5. verificarIntegridad() → cadena íntegra ────────────────────────────

    @Test
    @DisplayName("verificarIntegridad() retorna lista vacía para una cadena válida")
    void verificarIntegridad_cadenaIntegra_sinErrores() {
        // Construir dos registros encadenados correctamente
        LocalDateTime t1 = LocalDateTime.of(2026, 4, 27, 9, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 4, 27, 9, 1, 0);

        String hash1 = auditoriaService.calcularHash(GENESIS, "Inscripcion", 1L, "CREAR", "desc1", t1, 10L);
        String hash2 = auditoriaService.calcularHash(hash1,   "Materia",     2L, "MODIFICAR", "desc2", t2, 1L);

        RegistroAuditoria r1 = buildRegistro(1L, "Inscripcion", 1L, "CREAR",    "desc1", t1, 10L, GENESIS, hash1);
        RegistroAuditoria r2 = buildRegistro(2L, "Materia",     2L, "MODIFICAR","desc2", t2, 1L,  hash1,   hash2);

        when(auditoriaRepository.findAllByOrderByIdRegistroAsc()).thenReturn(List.of(r1, r2));

        List<String> errores = auditoriaService.verificarIntegridad();

        assertTrue(errores.isEmpty(), "No debe haber errores en una cadena válida");
    }

    // ─── 6. verificarIntegridad() detecta hashAnterior roto ──────────────────

    @Test
    @DisplayName("verificarIntegridad() detecta cuando hashAnterior no coincide")
    void verificarIntegridad_hashAnteriorRoto_reportaError() {
        LocalDateTime t1 = LocalDateTime.of(2026, 4, 27, 9, 0, 0);
        String hash1 = auditoriaService.calcularHash(GENESIS, "Inscripcion", 1L, "CREAR", "desc1", t1, 10L);

        RegistroAuditoria r1 = buildRegistro(1L, "Inscripcion", 1L, "CREAR", "desc1", t1, 10L, GENESIS, hash1);

        // r2 tiene un hashAnterior incorrecto (no es hash1)
        RegistroAuditoria r2 = buildRegistro(2L, "Materia", 2L, "MODIFICAR", "desc2",
            LocalDateTime.of(2026, 4, 27, 9, 1, 0), 1L, "hash_incorrecto", "cualquier_hash");

        when(auditoriaRepository.findAllByOrderByIdRegistroAsc()).thenReturn(List.of(r1, r2));

        List<String> errores = auditoriaService.verificarIntegridad();

        assertFalse(errores.isEmpty(), "Debe detectar error en la cadena");
        assertTrue(errores.stream().anyMatch(e -> e.contains("hashAnterior esperado")),
            "El error debe mencionar hashAnterior");
    }

    // ─── 7. verificarIntegridad() detecta datos manipulados ──────────────────

    @Test
    @DisplayName("verificarIntegridad() detecta cuando el hashActual no coincide con los datos")
    void verificarIntegridad_datosManipulados_reportaError() {
        LocalDateTime t1 = LocalDateTime.of(2026, 4, 27, 9, 0, 0);
        String hashCorrecto = auditoriaService.calcularHash(GENESIS, "Inscripcion", 1L, "CREAR", "original", t1, 10L);

        // El registro tiene descripcion diferente a la usada para calcular el hash
        RegistroAuditoria r1 = buildRegistro(1L, "Inscripcion", 1L, "CREAR",
            "MODIFICADO_POR_ATACANTE", t1, 10L, GENESIS, hashCorrecto);

        when(auditoriaRepository.findAllByOrderByIdRegistroAsc()).thenReturn(List.of(r1));

        List<String> errores = auditoriaService.verificarIntegridad();

        assertFalse(errores.isEmpty(), "Debe detectar manipulación de datos");
        assertTrue(errores.stream().anyMatch(e -> e.contains("DATOS MANIPULADOS")),
            "El error debe indicar manipulación");
    }

    // ─── 8. porEntidad() delega en repositorio ────────────────────────────────

    @Test
    @DisplayName("porEntidad() retorna registros filtrados por entidad")
    void porEntidad_delegaEnRepositorio() {
        RegistroAuditoria r1 = new RegistroAuditoria();
        r1.setEntidad("Inscripcion");
        when(auditoriaRepository.findByEntidadOrderByIdRegistroAsc("Inscripcion"))
            .thenReturn(List.of(r1));

        List<RegistroAuditoria> resultado = auditoriaService.porEntidad("Inscripcion");

        assertEquals(1, resultado.size());
        assertEquals("Inscripcion", resultado.get(0).getEntidad());
    }

    // ─── 9. porAccion() delega en repositorio ────────────────────────────────

    @Test
    @DisplayName("porAccion() retorna registros filtrados por acción")
    void porAccion_delegaEnRepositorio() {
        when(auditoriaRepository.findByAccionOrderByIdRegistroAsc("LOGIN")).thenReturn(List.of());

        List<RegistroAuditoria> resultado = auditoriaService.porAccion("LOGIN");

        assertTrue(resultado.isEmpty());
        verify(auditoriaRepository).findByAccionOrderByIdRegistroAsc("LOGIN");
    }

    // ─── 10. listarTodos() delega en repositorio ─────────────────────────────

    @Test
    @DisplayName("listarTodos() retorna todos los registros ordenados")
    void listarTodos_delegaEnRepositorio() {
        RegistroAuditoria r = new RegistroAuditoria();
        when(auditoriaRepository.findAllByOrderByIdRegistroAsc()).thenReturn(List.of(r));

        List<RegistroAuditoria> resultado = auditoriaService.listarTodos();

        assertEquals(1, resultado.size());
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private RegistroAuditoria buildRegistro(Long id, String entidad, Long idEntidad,
            String accion, String descripcion, LocalDateTime ts, Long idUsuario,
            String hashAnterior, String hashActual) {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setIdRegistro(id);
        r.setEntidad(entidad);
        r.setIdEntidad(idEntidad);
        r.setAccion(accion);
        r.setDescripcion(descripcion);
        r.setTimestampEvento(ts);
        r.setIdUsuario(idUsuario);
        r.setHashAnterior(hashAnterior);
        r.setHashActual(hashActual);
        return r;
    }
}
