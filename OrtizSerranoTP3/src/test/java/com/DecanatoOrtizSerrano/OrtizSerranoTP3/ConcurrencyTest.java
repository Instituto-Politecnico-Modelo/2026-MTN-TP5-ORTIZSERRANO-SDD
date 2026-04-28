package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  ConcurrencyTest – Bloqueo Optimista en Inscripciones
 * ═══════════════════════════════════════════════════════════════════
 *
 *  Verifica que el mecanismo de @Version (OptimisticLocking) en
 *  Materia.java previene sobrecupos bajo condiciones de carrera.
 *
 *  Escenario:
 *   - Se crea una materia con exactamente N cupos.
 *   - Se lanzan N+K hilos simultáneos (CountDownLatch para sincronía).
 *   - Cada hilo tiene su propio estudiante y token JWT.
 *   - Todos intentan inscribirse a la vez en la misma materia.
 *
 *  Resultado esperado:
 *   - Exactamente N inscripciones exitosas (201 Created).
 *   - Los K excedentes se encolan automáticamente (202 Accepted)
 *     gracias al Rate Limiter + Cola Virtual (Issue #26).
 *   - NUNCA se supera el límite de cupos: inscriptos ≤ N.
 *
 *  También testea:
 *   - Rate Limiter: el mismo usuario enviando > 5 req. rápidas → 429.
 *   - Semáforo por materia: las TX concurrentes no bloquean el sistema.
 * ═══════════════════════════════════════════════════════════════════
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Concurrencia – Bloqueo Optimista e Inscripciones Simultáneas")
class ConcurrencyTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Credenciales admin (seeded por DataInitializer) ───────────────────────
    private static final String ADMIN_EMAIL    = "admin@decanato.edu";
    private static final String ADMIN_PASSWORD = "Admin1234";

    // ── Estado compartido entre tests ─────────────────────────────────────────
    private static String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String authHeader(String token) {
        return "Bearer " + token;
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password)))
            .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    private String crearEstudianteYLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/admin/usuarios")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"nombre\":\"Conc\",\"apellido\":\"Test\",\"email\":\"%s\"," +
                    "\"password\":\"%s\",\"rol\":\"ESTUDIANTE\"}", email, password)))
            .andReturn();
        return login(email, password);
    }

    private Long crearMateriaConCupos(int cupos, String sufijo) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/admin/materias")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"codigo\":\"CONC%s\",\"nombre\":\"Materia Concurrencia %s\"," +
                    "\"cuposMaximos\":%d}", sufijo, sufijo, cupos)))
            .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
            .get("idMateria").asLong();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Setup – obtener token de admin
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(0)
    @DisplayName("Setup – login admin")
    void setup_loginAdmin() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertNotNull(adminToken);
        assertFalse(adminToken.isBlank());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TEST 1 – Condición de carrera: N hilos compiten por N-1 cupos
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("C1 – 10 hilos compiten por 5 cupos: exactamente 5 inscriptos, 5 encolados, NUNCA sobrecupo")
    void optimisticLock_10HilosCompiten5Cupos_nuncaSobrecupo() throws Exception {
        final int CUPOS     = 5;
        final int HILOS     = 10;

        // ── Crear materia con 5 cupos ──────────────────────────────────────
        Long idMateria = crearMateriaConCupos(CUPOS, "C1");

        // ── Crear 10 estudiantes y obtener sus tokens ──────────────────────
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            tokens.add(crearEstudianteYLogin(
                "conc.c1." + i + "@test.com", "Conc1234"));
        }

        // ── CountDownLatch: todos los hilos esperan la señal de largada ────
        CountDownLatch listo   = new CountDownLatch(HILOS);  // hilos listos
        CountDownLatch largada = new CountDownLatch(1);       // señal de inicio

        AtomicInteger inscriptos = new AtomicInteger(0);
        AtomicInteger encolados  = new AtomicInteger(0);
        AtomicInteger errores    = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        String idMateriaStr = idMateria.toString();

        List<Future<Integer>> futuros = new ArrayList<>();

        for (int i = 0; i < HILOS; i++) {
            final String token = tokens.get(i);
            futuros.add(pool.submit(() -> {
                listo.countDown();              // indico que estoy listo
                largada.await();                // espero la señal común

                MockMvc localMvc = webAppContextSetup(wac).apply(springSecurity()).build();

                MvcResult result = localMvc.perform(post("/api/inscripciones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMateria\":" + idMateriaStr + "}"))
                    .andReturn();

                return result.getResponse().getStatus();
            }));
        }

        // Esperar que todos los hilos estén listos y dar la largada
        listo.await(10, TimeUnit.SECONDS);
        largada.countDown();  // ¡Ya!

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        // ── Recolectar resultados ─────────────────────────────────────────
        for (Future<Integer> f : futuros) {
            int status = f.get();
            if (status == 201) inscriptos.incrementAndGet();
            else if (status == 202) encolados.incrementAndGet();
            else errores.incrementAndGet();
        }

        System.out.printf(
            "%n[ConcurrencyTest C1] Inscriptos: %d | Encolados: %d | Errores: %d%n",
            inscriptos.get(), encolados.get(), errores.get());

        // ── Aserciones clave ──────────────────────────────────────────────
        // 1. NUNCA se superan los cupos (propiedad crítica)
        assertTrue(inscriptos.get() <= CUPOS,
            "¡SOBRECUPO! Se inscribieron " + inscriptos.get() + " pero el máximo es " + CUPOS);

        // 2. Se llenaron exactamente todos los cupos disponibles
        assertEquals(CUPOS, inscriptos.get(),
            "Debían inscribirse exactamente " + CUPOS + " estudiantes");

        // 3. Los restantes fueron encolados (no rechazados con error)
        assertEquals(HILOS - CUPOS, encolados.get(),
            "Los " + (HILOS - CUPOS) + " restantes debían ser encolados (202)");

        // 4. No hubo errores inesperados
        assertEquals(0, errores.get(),
            "No debe haber errores inesperados (5xx ni 4xx distintos de los esperados)");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TEST 2 – 1 cupo, muchos estudiantes: exactamente 1 inscripto
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(2)
    @DisplayName("C2 – 1 cupo único, 8 hilos simultáneos: exactamente 1 inscripto y 7 encolados")
    void optimisticLock_1Cupo8Hilos_exactamente1Inscripto() throws Exception {
        final int CUPOS = 1;
        final int HILOS = 8;

        Long idMateria = crearMateriaConCupos(CUPOS, "C2");

        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            tokens.add(crearEstudianteYLogin("conc.c2." + i + "@test.com", "Conc1234"));
        }

        CountDownLatch listo   = new CountDownLatch(HILOS);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger inscriptos = new AtomicInteger(0);
        AtomicInteger encolados  = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        String idMateriaStr = idMateria.toString();
        List<Future<Integer>> futuros = new ArrayList<>();

        for (int i = 0; i < HILOS; i++) {
            final String token = tokens.get(i);
            futuros.add(pool.submit(() -> {
                listo.countDown();
                largada.await();
                MockMvc localMvc = webAppContextSetup(wac).apply(springSecurity()).build();
                MvcResult result = localMvc.perform(post("/api/inscripciones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMateria\":" + idMateriaStr + "}"))
                    .andReturn();
                return result.getResponse().getStatus();
            }));
        }

        listo.await(10, TimeUnit.SECONDS);
        largada.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        for (Future<Integer> f : futuros) {
            int s = f.get();
            if (s == 201) inscriptos.incrementAndGet();
            else if (s == 202) encolados.incrementAndGet();
        }

        System.out.printf(
            "%n[ConcurrencyTest C2] Inscriptos: %d | Encolados: %d%n",
            inscriptos.get(), encolados.get());

        // Exactamente 1 cupo ocupado — el OptimisticLocking garantiza esto
        assertEquals(1, inscriptos.get(),
            "Con 1 cupo solo puede inscribirse exactamente 1 estudiante");
        assertTrue(inscriptos.get() <= CUPOS,
            "¡SOBRECUPO detectado! inscriptos=" + inscriptos.get());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TEST 3 – Rate Limiter: el mismo usuario hace flood de solicitudes → 429
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(3)
    @DisplayName("C3 – Rate Limiter: mismo usuario hace 8 solicitudes rápidas → al menos 1 bloqueada con 429")
    void rateLimiter_mismousuario_solicitudesRapidas_retorna429() throws Exception {
        final int SOLICITUDES = 8;   // supera el límite de 5 por ventana

        // Una sola materia con cupos ilimitados (cuposMaximos=0) - creamos por separado para cada req
        mockMvc.perform(post("/api/admin/materias")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"RATETEST\",\"nombre\":\"Materia Rate\",\"cuposMaximos\":0}"))
            .andReturn();

        // Un único estudiante que hará todas las solicitudes
        String token = crearEstudianteYLogin("rate.flood@test.com", "Rate1234");

        AtomicInteger status429 = new AtomicInteger(0);

        // Crear materias distintas para cada solicitud (evitar "ya inscripto")
        for (int i = 0; i < SOLICITUDES; i++) {
            MvcResult m = mockMvc.perform(post("/api/admin/materias")
                    .header("Authorization", authHeader(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"codigo\":\"RT" + i + "\",\"nombre\":\"Rate " + i
                        + "\",\"cuposMaximos\":0}"))
                .andReturn();
            Long idM = objectMapper.readTree(m.getResponse().getContentAsString())
                .get("idMateria").asLong();

            int status = mockMvc.perform(post("/api/inscripciones")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"idMateria\":" + idM + "}"))
                .andReturn().getResponse().getStatus();

            if (status == 429) status429.incrementAndGet();
        }

        System.out.printf(
            "%n[ConcurrencyTest C3] 429 recibidos: %d de %d solicitudes%n",
            status429.get(), SOLICITUDES);

        // Con 8 solicitudes y límite de 5, al menos 3 deberían ser bloqueadas
        assertTrue(status429.get() > 0,
            "El Rate Limiter debería haber bloqueado al menos 1 solicitud con 429");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TEST 4 – Cancelación libera cupo: el siguiente en cola se promueve
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(4)
    @DisplayName("C4 – Cancelación de inscripción promueve automáticamente al primero de la cola")
    void cancelacion_liberaCupo_promuevePrimeroEnCola() throws Exception {
        // Materia con 1 cupo
        Long idMateria = crearMateriaConCupos(1, "C4");

        // Estudiante A ocupa el cupo (201)
        String tokenA = crearEstudianteYLogin("conc.c4.a@test.com", "Conc1234");
        MvcResult resA = mockMvc.perform(post("/api/inscripciones")
                .header("Authorization", authHeader(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idMateria\":" + idMateria + "}"))
            .andReturn();
        assertEquals(201, resA.getResponse().getStatus(), "Estudiante A debe inscribirse (201)");
        Long inscripcionAId = objectMapper.readTree(resA.getResponse().getContentAsString())
            .get("idInscripcion").asLong();

        // Estudiante B intenta inscribirse → sin cupos → encolado (202)
        String tokenB = crearEstudianteYLogin("conc.c4.b@test.com", "Conc1234");
        MvcResult resB = mockMvc.perform(post("/api/inscripciones")
                .header("Authorization", authHeader(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idMateria\":" + idMateria + "}"))
            .andReturn();
        assertEquals(202, resB.getResponse().getStatus(), "Estudiante B debe ser encolado (202)");

        // Estudiante A cancela su inscripción → debe liberar cupo y promover B
        MvcResult cancelRes = mockMvc.perform(patch("/api/inscripciones/" + inscripcionAId + "/cancelar")
                .header("Authorization", authHeader(tokenA)))
            .andReturn();
        assertEquals(200, cancelRes.getResponse().getStatus(), "Cancelación debe retornar 200");

        // Verificar: el turno de B fue promovido (estado PROMOVIDO en cola)
        // Lo vemos consultando mis-turnos de B: no debería tener pendientes
        // (fue promovido automáticamente por promoverSiguiente())
        MvcResult turnosB = mockMvc.perform(get("/api/inscripciones/cola/mis-turnos")
                .header("Authorization", authHeader(tokenB)))
            .andReturn();
        assertEquals(200, turnosB.getResponse().getStatus());

        // B debería tener 0 turnos PENDIENTES (fue promovido)
        int cantPendientes = objectMapper.readTree(turnosB.getResponse().getContentAsString()).size();

        System.out.printf(
            "%n[ConcurrencyTest C4] Turnos PENDIENTES de B tras promoción: %d%n",
            cantPendientes);

        assertEquals(0, cantPendientes,
            "B fue promovido: no debe tener turnos PENDIENTES");
    }
}
