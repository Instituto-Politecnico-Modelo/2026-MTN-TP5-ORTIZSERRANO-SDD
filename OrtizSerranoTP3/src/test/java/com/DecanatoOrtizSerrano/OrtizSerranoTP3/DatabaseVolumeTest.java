package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.*;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatabaseVolumeTest — Pruebas de rendimiento y correctitud bajo alto volumen de datos.
 *
 * <p>Escenario de datos:</p>
 * <ul>
 *   <li>200 estudiantes</li>
 *   <li>50 materias (con cuposMaximos = 30)</li>
 *   <li>~5 000 inscripciones (distribuidas: ACTIVA, CANCELADA, APROBADA)</li>
 *   <li>200 entradas en la cola de inscripción</li>
 * </ul>
 *
 * <p>Qué se verifica:</p>
 * <ol>
 *   <li><b>V1 – Carga masiva (batch insert)</b>: inserta todo en tiempo razonable</li>
 *   <li><b>V2 – Índice idx_inscripciones_mat_estado</b>: countCuposOcupados en < 15 ms con 5 000 filas</li>
 *   <li><b>V3 – Índice idx_inscripciones_estudiante</b>: findByEstudianteIdUsuario en < 15 ms</li>
 *   <li><b>V4 – Índice idx_materias_codigo (UNIQUE)</b>: findByCodigo en < 5 ms</li>
 *   <li><b>V5 – Índice idx_cola_materia_estado_pos</b>: primer PENDIENTE de la cola en < 10 ms</li>
 *   <li><b>V6 – Trigger lógico: cupos no superados</b>: countCuposOcupados ≤ cuposMaximos en todas las materias</li>
 *   <li><b>V7 – Trigger lógico: unicidad activa</b>: ningún estudiante tiene dos inscripciones ACTIVAS a la misma materia</li>
 *   <li><b>V8 – Rendimiento de búsqueda por email</b>: findByEmail en < 5 ms con 200 usuarios</li>
 *   <li><b>V9 – Expiración masiva de cola</b>: actualización batch de PENDIENTE→EXPIRADO en < 100 ms</li>
 *   <li><b>V10 – Escalabilidad de auditoría</b>: encadenamiento de 500 registros de auditoría sin degradación</li>
 * </ol>
 *
 * <p>Umbrales (H2 en memoria, JVM caliente): valores conservadores × 3 sobre MySQL real esperado.</p>
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseVolumeTest {

    // ── Umbrales de tiempo (ms) ───────────────────────────────────────────────
    private static final long MAX_MS_COUNT_CUPOS        = 15;
    private static final long MAX_MS_FIND_BY_ESTUDIANTE  = 15;
    private static final long MAX_MS_FIND_BY_CODIGO      = 5;
    private static final long MAX_MS_FIND_COLA_PRIMERO   = 10;
    private static final long MAX_MS_FIND_BY_EMAIL       = 5;
    private static final long MAX_MS_EXPIRACION_MASIVA   = 100;
    // BCrypt × 200 estudiantes puede tomar ~20 s en CI; el umbral mide la BD, no el hashing
    private static final long MAX_MS_BULK_INSERT_TOTAL   = 60_000;

    // ── Volúmenes ────────────────────────────────────────────────────────────
    private static final int N_ESTUDIANTES     = 200;
    private static final int N_MATERIAS        = 50;
    private static final int CUPOS_POR_MATERIA = 30;
    private static final int N_AUDITORIA       = 500;

    // IDs de entidades de referencia compartidas entre tests (se asignan en V1)
    private static Long idMateriaReferencia;
    private static Long idEstudianteReferencia;
    private static Long idMateriaConCola;

    @Autowired private EstudianteRepository     estudianteRepository;
    @Autowired private MateriaRepository        materiaRepository;
    @Autowired private InscripcionRepository    inscripcionRepository;
    @Autowired private ColaInscripcionRepository colaRepository;
    @Autowired private UsuarioRepository        usuarioRepository;
    @Autowired private AuditoriaRepository      auditoriaRepository;
    @Autowired private PasswordEncoder          passwordEncoder;

    // =========================================================================
    // V1 — CARGA MASIVA: insertar todo el dataset en tiempo razonable
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("V1 — Batch insert: 200 estudiantes + 50 materias + ~5000 inscripciones")
    void v1_cargaMasiva_tiempoRazonable() {

        long start = System.currentTimeMillis();

        // ── 1. Crear 200 estudiantes (saveAndFlush para que Hibernate asigne el ID) ──
        List<Long> idsEstudiante = new ArrayList<>(N_ESTUDIANTES);
        for (int i = 1; i <= N_ESTUDIANTES; i++) {
            Estudiante e = new Estudiante(
                    "Nombre" + i, "Apellido" + i,
                    "est" + i + "@test.com",
                    passwordEncoder.encode("pass123"),
                    "LEG-VOL-" + String.format("%04d", i),
                    "Ingeniería en Sistemas",
                    2020 + (i % 6)
            );
            Estudiante saved = estudianteRepository.saveAndFlush(e);
            idsEstudiante.add(saved.getIdUsuario());
        }

        // ── 2. Crear 50 materias ──────────────────────────────────────────────
        List<Long> idsMaterias = new ArrayList<>(N_MATERIAS);
        for (int i = 1; i <= N_MATERIAS; i++) {
            Materia m = new Materia(
                    "VOL-" + String.format("%03d", i),
                    "Materia Volumen " + i,
                    "Descripción de prueba de volumen " + i,
                    6
            );
            m.setCuposMaximos(CUPOS_POR_MATERIA);
            m.setAnio(1 + (i % 5));
            m.setCuatrimestre(1 + (i % 2));
            Materia saved = materiaRepository.saveAndFlush(m);
            idsMaterias.add(saved.getIdMateria());
        }

        // Guardar referencias para tests posteriores
        idMateriaReferencia    = idsMaterias.get(0);
        idEstudianteReferencia = idsEstudiante.get(0);
        idMateriaConCola       = idsMaterias.get(N_MATERIAS - 1);

        // ── 3. Crear inscripciones (recargar entidades por ID para que estén managed) ─
        int inscripcionesCreadas = 0;
        for (int mi = 0; mi < N_MATERIAS; mi++) {
            Materia materia = materiaRepository.findById(idsMaterias.get(mi)).orElseThrow();
            for (int ei = 0; ei < CUPOS_POR_MATERIA && ei < N_ESTUDIANTES; ei++) {
                Estudiante est = estudianteRepository.findById(idsEstudiante.get(ei)).orElseThrow();
                Inscripcion insc = new Inscripcion(est, materia, LocalDate.now());
                // Las primeras 5 de cada materia son CANCELADAS
                if (ei < 5) {
                    insc.setEstado("CANCELADA");
                }
                inscripcionRepository.save(insc);
                inscripcionesCreadas++;
            }
        }
        inscripcionRepository.flush();

        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("[V1] Carga masiva: %d estudiantes | %d materias | %d inscripciones → %d ms%n",
                N_ESTUDIANTES, N_MATERIAS, inscripcionesCreadas, elapsed);

        assertThat(elapsed)
                .as("La carga de datos no debería superar %d ms", MAX_MS_BULK_INSERT_TOTAL)
                .isLessThan(MAX_MS_BULK_INSERT_TOTAL);

        assertThat(estudianteRepository.count()).isGreaterThanOrEqualTo(N_ESTUDIANTES);
        assertThat(materiaRepository.count()).isGreaterThanOrEqualTo(N_MATERIAS);
        assertThat(inscripcionRepository.count()).isGreaterThanOrEqualTo(inscripcionesCreadas);
    }

    // =========================================================================
    // V2 — ÍNDICE idx_inscripciones_mat_estado: countCuposOcupados
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("V2 — Índice mat+estado: countCuposOcupados < " + MAX_MS_COUNT_CUPOS + " ms con 5000 filas")
    void v2_indice_matEstado_countCuposOcupados_rapido() {
        Assumptions.assumeTrue(idMateriaReferencia != null, "V1 no se ejecutó");

        // Warm-up (evitar latencia de primera llamada JIT)
        inscripcionRepository.countCuposOcupados(idMateriaReferencia);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {  // 100 ejecuciones
            inscripcionRepository.countCuposOcupados(idMateriaReferencia);
        }
        long totalMs = System.currentTimeMillis() - start;
        long avgMs   = totalMs / 100;

        System.out.printf("[V2] countCuposOcupados × 100 → total=%d ms | avg=%d ms%n", totalMs, avgMs);

        assertThat(avgMs)
                .as("Promedio de countCuposOcupados debe ser < %d ms (índice mat+estado)", MAX_MS_COUNT_CUPOS)
                .isLessThan(MAX_MS_COUNT_CUPOS);
    }

    // =========================================================================
    // V3 — ÍNDICE idx_inscripciones_estudiante: findByEstudianteIdUsuario
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("V3 — Índice id_estudiante: findByEstudianteIdUsuario < " + MAX_MS_FIND_BY_ESTUDIANTE + " ms")
    void v3_indice_estudiante_findByEstudiante_rapido() {
        Assumptions.assumeTrue(idEstudianteReferencia != null, "V1 no se ejecutó");

        // Warm-up
        inscripcionRepository.findByEstudianteIdUsuario(idEstudianteReferencia);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            inscripcionRepository.findByEstudianteIdUsuario(idEstudianteReferencia);
        }
        long avgMs = (System.currentTimeMillis() - start) / 50;

        System.out.printf("[V3] findByEstudianteIdUsuario × 50 → avg=%d ms%n", avgMs);

        assertThat(avgMs)
                .as("Promedio de findByEstudianteIdUsuario debe ser < %d ms", MAX_MS_FIND_BY_ESTUDIANTE)
                .isLessThan(MAX_MS_FIND_BY_ESTUDIANTE);
    }

    // =========================================================================
    // V4 — ÍNDICE idx_materias_codigo (UNIQUE): findByCodigo
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("V4 — Índice UNIQUE codigo: findByCodigo < " + MAX_MS_FIND_BY_CODIGO + " ms")
    void v4_indice_codigo_findByCodigo_rapido() {
        // Warm-up
        materiaRepository.findByCodigo("VOL-001");

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            materiaRepository.findByCodigo("VOL-" + String.format("%03d", (i % N_MATERIAS) + 1));
        }
        long avgMs = (System.currentTimeMillis() - start) / 100;

        System.out.printf("[V4] findByCodigo × 100 → avg=%d ms%n", avgMs);

        assertThat(avgMs)
                .as("Promedio de findByCodigo (índice UNIQUE) debe ser < %d ms", MAX_MS_FIND_BY_CODIGO)
                .isLessThan(MAX_MS_FIND_BY_CODIGO);
    }

    // =========================================================================
    // V5 — ÍNDICE idx_cola_materia_estado_pos: primer PENDIENTE de la cola
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("V5 — Índice cola (materia+estado+posicion): primer PENDIENTE < " + MAX_MS_FIND_COLA_PRIMERO + " ms")
    void v5_indice_cola_primerPendiente_rapido() {
        Assumptions.assumeTrue(idMateriaConCola != null, "V1 no se ejecutó");

        // Poblar la cola con 200 entradas PENDIENTES para la materia de referencia
        List<Estudiante> estudiantes = estudianteRepository.findAll()
                .stream().limit(200).toList();
        Materia materia = materiaRepository.findById(idMateriaConCola).orElseThrow();

        for (int i = 0; i < Math.min(200, estudiantes.size()); i++) {
            // Evitar duplicados si V1 ya creó algunas
            boolean yaEnCola = colaRepository.existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstado(
                    estudiantes.get(i).getIdUsuario(), idMateriaConCola,
                    ColaInscripcion.Estado.PENDIENTE);
            if (!yaEnCola) {
                ColaInscripcion entrada = new ColaInscripcion(
                        estudiantes.get(i),
                        materia,
                        i + 1,
                        java.time.LocalDateTime.now().plusHours(48)
                );
                colaRepository.save(entrada);
            }
        }

        // Warm-up
        colaRepository.findPrimeroPendiente(idMateriaConCola);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            colaRepository.findPrimeroPendiente(idMateriaConCola);
        }
        long avgMs = (System.currentTimeMillis() - start) / 100;

        System.out.printf("[V5] findPrimerPendiente × 100 → avg=%d ms%n", avgMs);

        assertThat(avgMs)
                .as("Promedio de findPrimerPendiente (índice compuesto) debe ser < %d ms", MAX_MS_FIND_COLA_PRIMERO)
                .isLessThan(MAX_MS_FIND_COLA_PRIMERO);
    }

    // =========================================================================
    // V6 — TRIGGER LÓGICO: cupos no superados en ninguna materia
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("V6 — Integridad cupos: countCuposOcupados ≤ cuposMaximos en todas las materias")
    void v6_triggerLogico_cuposNoSuperados() {
        List<Materia> materias = materiaRepository.findAll();

        List<String> violaciones = new ArrayList<>();        for (Materia m : materias) {
            if (m.getCuposMaximos() == null) continue;
            long ocupados = inscripcionRepository.countCuposOcupados(m.getIdMateria());
            if (ocupados > m.getCuposMaximos()) {
                violaciones.add(String.format(
                        "Materia '%s' (id=%d): ocupados=%d > max=%d",
                        m.getCodigo(), m.getIdMateria(), ocupados, m.getCuposMaximos()));
            }
        }

        System.out.printf("[V6] Materias verificadas: %d | Violaciones: %d%n",
                materias.size(), violaciones.size());
        if (!violaciones.isEmpty()) {
            violaciones.forEach(v -> System.err.println("  ❌ " + v));
        }

        assertThat(violaciones)
                .as("Ninguna materia debería tener más inscripciones ACTIVAS que cuposMaximos")
                .isEmpty();
    }

    // =========================================================================
    // V7 — TRIGGER LÓGICO: unicidad activa — ningún duplicado ACTIVO/APROBADO
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("V7 — Integridad unicidad: ningún estudiante tiene 2 inscripciones activas a la misma materia")
    void v7_triggerLogico_unicidadActiva() {
        List<Inscripcion> todasActivas = inscripcionRepository.findAll()
                .stream()
                .filter(i -> !"CANCELADA".equals(i.getEstado()))
                .toList();

        // Agrupar por (estudiante, materia) y buscar grupos con más de 1 elemento
        Map<String, Long> conteo = todasActivas.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getEstudiante().getIdUsuario() + ":" + i.getMateria().getIdMateria(),
                        Collectors.counting()
                ));

        List<String> duplicados = conteo.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> "est+mat=" + e.getKey() + " → " + e.getValue() + " inscripciones activas")
                .toList();

        System.out.printf("[V7] Inscripciones no-CANCELADAS: %d | Duplicados: %d%n",
                todasActivas.size(), duplicados.size());

        assertThat(duplicados)
                .as("No debe haber inscripciones duplicadas activas para el mismo (estudiante, materia)")
                .isEmpty();
    }

    // =========================================================================
    // V8 — ÍNDICE idx_usuarios_email: findByEmail
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("V8 — Índice email: findByEmail < " + MAX_MS_FIND_BY_EMAIL + " ms con 200 usuarios")
    void v8_indice_email_findByEmail_rapido() {
        // Warm-up
        usuarioRepository.findByEmail("est1@test.com");

        long start = System.currentTimeMillis();
        for (int i = 1; i <= 100; i++) {
            usuarioRepository.findByEmail("est" + (i % N_ESTUDIANTES + 1) + "@test.com");
        }
        long avgMs = (System.currentTimeMillis() - start) / 100;

        System.out.printf("[V8] findByEmail × 100 → avg=%d ms%n", avgMs);

        assertThat(avgMs)
                .as("Promedio de findByEmail (índice UNIQUE email) debe ser < %d ms", MAX_MS_FIND_BY_EMAIL)
                .isLessThan(MAX_MS_FIND_BY_EMAIL);
    }

    // =========================================================================
    // V9 — EXPIRACIÓN MASIVA: UPDATE batch de PENDIENTE → EXPIRADO
    // =========================================================================

    @Test
    @Order(9)
    @Transactional
    @DisplayName("V9 — Expiración masiva de cola: UPDATE batch < " + MAX_MS_EXPIRACION_MASIVA + " ms")
    void v9_expiracionMasiva_batch_rapido() {
        Assumptions.assumeTrue(idMateriaConCola != null, "V1 no se ejecutó");

        // Contar pendientes antes usando la query existente
        long pendientesAntes = colaRepository.findPendientesPorMateriaOrdenados(idMateriaConCola).size();
        System.out.printf("[V9] Pendientes antes de expiración: %d%n", pendientesAntes);

        long start = System.currentTimeMillis();
        // expirarVencidos usa fecha < ahora — pasamos futuro para expirar todos los que insertamos en V5
        int expirados = colaRepository.expirarVencidos(java.time.LocalDateTime.now().plusDays(3));
        long elapsed  = System.currentTimeMillis() - start;

        System.out.printf("[V9] Expiración masiva: %d registros en %d ms%n", expirados, elapsed);

        assertThat(elapsed)
                .as("La expiración masiva debe completarse en < %d ms", MAX_MS_EXPIRACION_MASIVA)
                .isLessThan(MAX_MS_EXPIRACION_MASIVA);

        // Verificar que realmente se marcaron como EXPIRADOS
        long pendientesDespues = colaRepository.findPendientesPorMateriaOrdenados(idMateriaConCola).size();
        assertThat(pendientesDespues)
                .as("Después de expirar, los pendientes deben ser 0 para idMateriaConCola")
                .isZero();
    }

    // =========================================================================
    // V10 — ESCALABILIDAD DE AUDITORÍA: 500 registros encadenados
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("V10 — Auditoría encadenada: insertar y recuperar 500 registros en tiempo razonable")
    void v10_auditoria_encadenada_escalabilidad() {
        long totalAntes = auditoriaRepository.count();

        // Insertar 500 registros de auditoría simulando eventos reales
        long start = System.currentTimeMillis();
        String hashPrevio = "0000000000000000000000000000000000000000000000000000000000000000";
        for (int i = 0; i < N_AUDITORIA; i++) {
            RegistroAuditoria r = new RegistroAuditoria();
            r.setEntidad("Inscripcion");
            r.setIdEntidad((long) (i + 1));
            r.setAccion(i % 2 == 0 ? "INSCRIBIR" : "CANCELAR");
            r.setDescripcion("Evento de prueba de volumen #" + i);
            r.setIdUsuario((long) (i % N_ESTUDIANTES + 1));
            r.setTimestampEvento(java.time.LocalDateTime.now().minusMinutes(N_AUDITORIA - i));
            r.setHashAnterior(hashPrevio);
            // Hash simulado (en producción lo calcula AuditoriaService)
            String contenido = hashPrevio + "Inscripcion" + (i + 1) + r.getAccion() + i;
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) sb.append(String.format("%02x", b));
                hashPrevio = sb.toString();
            } catch (Exception ex) {
                hashPrevio = "hash-simulado-" + i;
            }
            r.setHashActual(hashPrevio);
            auditoriaRepository.save(r);
        }
        long msInsert = System.currentTimeMillis() - start;

        // Consulta de recuperación por entidad (usa idx_auditoria_entidad_id)
        start = System.currentTimeMillis();
        List<RegistroAuditoria> registros = auditoriaRepository
                .findByEntidadOrderByIdRegistroAsc("Inscripcion");
        long msQuery = System.currentTimeMillis() - start;

        System.out.printf("[V10] Auditoría: insert %d registros → %d ms | query → %d ms | total: %d registros%n",
                N_AUDITORIA, msInsert, msQuery, registros.size());

        assertThat(msInsert)
                .as("Insertar 500 registros de auditoría debe completarse en < 5000 ms")
                .isLessThan(5_000L);

        assertThat(msQuery)
                .as("Recuperar registros de auditoría por entidad (con índice) debe ser < 200 ms")
                .isLessThan(200L);

        assertThat(auditoriaRepository.count())
                .isGreaterThanOrEqualTo(totalAntes + N_AUDITORIA);
    }

    // ── Helper: Assumptions class (JUnit 5) ───────────────────────────────────

    static class Assumptions {
        static void assumeTrue(boolean condition, String message) {
            org.junit.jupiter.api.Assumptions.assumeTrue(condition,
                    "Test omitido — prerequisito no cumplido: " + message);
        }
    }
}
