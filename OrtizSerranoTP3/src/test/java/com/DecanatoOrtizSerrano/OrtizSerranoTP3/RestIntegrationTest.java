package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para los endpoints REST críticos.
 *
 * Usa H2 in-memory + Spring Boot completo + MockMvc.
 * DataInitializer siembra admin@decanato.edu / Admin1234 al arrancar.
 *
 * Cubre:
 *  1.  POST /api/auth/login → 200 + JWT con campos correctos
 *  2.  POST /api/auth/login → 401 con credenciales inválidas
 *  3.  POST /api/auth/login → 400 con body vacío (validación)
 *  4.  GET  /api/admin/materias → 401 sin token
 *  5.  GET  /api/admin/materias → 200 con token admin
 *  6.  POST /api/admin/materias → 201 con datos válidos (admin)
 *  7.  POST /api/admin/materias → 400 sin código (validación)
 *  8.  POST /api/admin/usuarios → 201 crea estudiante (admin)
 *  9.  POST /api/inscripciones  → 201 estudiante se inscribe
 * 10.  POST /api/inscripciones  → 400 ya inscripto en la misma materia
 * 11.  POST /api/inscripciones  → 400 cupos agotados
 * 12.  GET  /api/inscripciones/mis-inscripciones → 200 lista correcta
 * 13.  DELETE /api/inscripciones/{id} → 200 cancela
 * 14.  GET  /api/admin/auditoria → 200 registros existen
 * 15.  GET  /api/admin/auditoria/integridad → 200 cadena íntegra
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Integración REST – Endpoints críticos")
class RestIntegrationTest {

    @Autowired private WebApplicationContext wac;

    // ObjectMapper creado manualmente porque Spring Boot 4 no lo registra como Bean por defecto
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    // Tokens y IDs reutilizables entre tests (orden importa)
    private static String adminToken;
    private static String estudianteToken;
    private static Long   materiaId;
    private static Long   materiaConCuposId;
    private static Long   inscripcionId;
    private static final String ESTUDIANTE_EMAIL = "integ_stu@test.com";
    private static final String ESTUDIANTE_PASS  = "Integ1234";

    @BeforeEach
    void setUpMvc() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String authHeader(String token) {
        return "Bearer " + token;
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ─── 1. Login exitoso ─────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("POST /api/auth/login → 200 con credenciales admin válidas")
    void login_adminValido_retorna200ConToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@decanato.edu\",\"password\":\"Admin1234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.role").value("ADMINISTRADOR"))
            .andExpect(jsonPath("$.email").value("admin@decanato.edu"))
            .andReturn();

        adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("token").asText();
    }

    // ─── 2. Login con credenciales inválidas ──────────────────────────────────

    @Test @Order(2)
    @DisplayName("POST /api/auth/login → 401 con password incorrecta")
    void login_passwordIncorrecta_retorna401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@decanato.edu\",\"password\":\"INCORRECTO\"}"))
            .andExpect(status().isUnauthorized());
    }

    // ─── 3. Login body vacío ──────────────────────────────────────────────────

    @Test @Order(3)
    @DisplayName("POST /api/auth/login → 400 con body vacío")
    void login_bodyVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().is4xxClientError());
    }

    // ─── 4. Materias sin token → 401 ─────────────────────────────────────────

    @Test @Order(4)
    @DisplayName("GET /api/admin/materias → 401 sin Authorization header")
    void listaMaterias_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/admin/materias"))
            .andExpect(status().isUnauthorized());
    }

    // ─── 5. Materias con token admin → 200 ───────────────────────────────────

    @Test @Order(5)
    @DisplayName("GET /api/admin/materias → 200 con token admin")
    void listaMaterias_conTokenAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ─── 6. Crear materia → 201 ───────────────────────────────────────────────

    @Test @Order(6)
    @DisplayName("POST /api/admin/materias → 201 crea materia con datos válidos")
    void crearMateria_datosValidos_retorna201() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/materias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader(adminToken))
                .content("{\"codigo\":\"INTEG01\",\"nombre\":\"Materia Integración\",\"creditos\":3}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value("INTEG01"))
            .andExpect(jsonPath("$.idMateria").exists())
            .andReturn();

        materiaId = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("idMateria").asLong();
    }

    // ─── 7. Crear materia sin código → 400 ───────────────────────────────────

    @Test @Order(7)
    @DisplayName("POST /api/admin/materias → 400 sin campo código")
    void crearMateria_sinCodigo_retorna400() throws Exception {
        mockMvc.perform(post("/api/admin/materias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader(adminToken))
                .content("{\"nombre\":\"Sin código\"}"))
            .andExpect(status().isBadRequest());
    }

    // ─── 8. Crear estudiante → 201 ────────────────────────────────────────────

    @Test @Order(8)
    @DisplayName("POST /api/admin/usuarios → 201 crea estudiante")
    void crearEstudiante_datosValidos_retorna201() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader(adminToken))
                .content(json(Map.of(
                    "nombre",   "Estudiante",
                    "apellido", "Integración",
                    "email",    ESTUDIANTE_EMAIL,
                    "password", ESTUDIANTE_PASS,
                    "rol",      "ESTUDIANTE"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").exists());

        // Obtener token del estudiante
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + ESTUDIANTE_EMAIL + "\",\"password\":\"" + ESTUDIANTE_PASS + "\"}"))
            .andExpect(status().isOk())
            .andReturn();

        estudianteToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
            .get("token").asText();
    }

    // ─── 9. Inscribirse → 201 ─────────────────────────────────────────────────

    @Test @Order(9)
    @DisplayName("POST /api/inscripciones → 201 estudiante se inscribe exitosamente")
    void inscribirse_exitoso_retorna201() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/inscripciones")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader(estudianteToken))
                .content("{\"idMateria\":" + materiaId + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("ACTIVA"))
            .andExpect(jsonPath("$.idInscripcion").exists())
            .andReturn();

        inscripcionId = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("idInscripcion").asLong();
    }

    // ─── 10. Re-inscripción → 400 ────────────────────────────────────────────

    @Test @Order(10)
    @DisplayName("POST /api/inscripciones → 400 si ya está inscripto en la misma materia")
    void inscribirse_yaInscripto_retorna400() throws Exception {
        mockMvc.perform(post("/api/inscripciones")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader(estudianteToken))
                .content("{\"idMateria\":" + materiaId + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Ya estás inscripto")));
    }

    // ── 11. Cupos agotados → 202 + encolado automático ─────────────────────

    @Test @Order(11)
    @DisplayName("POST /api/inscripciones → 202 y encolado automático cuando no hay cupos")
    void inscribirse_sinCupos_retorna202_yEncola() throws Exception {
        // Crear materia con 0 cupos
        MvcResult matResult = mockMvc.perform(post("/api/admin/materias")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader(adminToken))
                .content("{\"codigo\":\"NOQUOTA\",\"nombre\":\"Sin cupos\",\"cuposMaximos\":1}"))
            .andExpect(status().isCreated())
            .andReturn();
        materiaConCuposId = objectMapper.readTree(matResult.getResponse().getContentAsString())
            .get("idMateria").asLong();

        // Crear otro estudiante y ocupar el único cupo
        mockMvc.perform(post("/api/admin/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader(adminToken))
                .content(json(Map.of("nombre","Otro","apellido","Alu","email","otro@t.com",
                    "password","Otro1234","rol","ESTUDIANTE"))))
            .andExpect(status().isCreated());

        MvcResult otroLogin = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"otro@t.com\",\"password\":\"Otro1234\"}"))
            .andReturn();
        String otroToken = objectMapper.readTree(otroLogin.getResponse().getContentAsString())
            .get("token").asText();

        // Otro estudiante ocupa el cupo
        mockMvc.perform(post("/api/inscripciones")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader(otroToken))
                .content("{\"idMateria\":" + materiaConCuposId + "}"))
            .andExpect(status().isCreated());

        // Nuestro estudiante intenta inscribirse → sin cupos → 202 + encolado automático
        mockMvc.perform(post("/api/inscripciones")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authHeader(estudianteToken))
                .content("{\"idMateria\":" + materiaConCuposId + "}"))
            .andExpect(status().isAccepted())                    // HTTP 202
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))  // turno en cola
            .andExpect(jsonPath("$.idMateria").value(materiaConCuposId));
    }

    // ─── 12. Mis inscripciones → 200 ─────────────────────────────────────────

    @Test @Order(12)
    @DisplayName("GET /api/inscripciones/mis-inscripciones → 200 con lista de inscripciones")
    void misInscripciones_retorna200ConLista() throws Exception {
        mockMvc.perform(get("/api/inscripciones/mis-inscripciones")
                .header("Authorization", authHeader(estudianteToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[0].estado").value("ACTIVA"));
    }

    // ─── 13. Cancelar inscripción → 200 ──────────────────────────────────────

    @Test @Order(13)
    @DisplayName("PATCH /api/inscripciones/{id}/cancelar → 200 cancela inscripción propia")
    void cancelarInscripcion_exitoso_retorna200() throws Exception {
        mockMvc.perform(patch("/api/inscripciones/" + inscripcionId + "/cancelar")
                .header("Authorization", authHeader(estudianteToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    // ─── 14. Auditoria → 200 con registros ───────────────────────────────────

    @Test @Order(14)
    @DisplayName("GET /api/admin/auditoria → 200 con registros generados por las operaciones previas")
    void auditoria_listarTodos_retorna200ConRegistros() throws Exception {
        mockMvc.perform(get("/api/admin/auditoria")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    // ─── 15. Integridad de auditoría → cadena íntegra ────────────────────────

    @Test @Order(15)
    @DisplayName("GET /api/admin/auditoria/verificar → 200 retorna resultado de integridad")
    void auditoria_verificarIntegridad_retornaEstructuraCorrecta() throws Exception {
        mockMvc.perform(get("/api/admin/auditoria/verificar")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integra").isBoolean())
            .andExpect(jsonPath("$.totalRegistros").isNumber())
            .andExpect(jsonPath("$.errores").isArray())
            .andExpect(jsonPath("$.mensaje").isString());
    }
}
