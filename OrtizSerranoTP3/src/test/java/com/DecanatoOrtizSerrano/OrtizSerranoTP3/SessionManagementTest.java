package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SM – Session Management Tests
 *
 * Verifica el manejo de sesión y control de acceso por rol.
 * Usa H2 in-memory + Spring Boot completo + MockMvc.
 * DataInitializer siembra admin@decanato.edu / Admin1234 al arrancar.
 *
 *  SM01 – Login válido devuelve 200 y token no nulo
 *  SM02 – Ruta protegida sin token devuelve 401
 *  SM03 – Ruta protegida con token inválido/corrupto devuelve 401
 *  SM04 – Admin accede a /api/admin/materias → 200
 *  SM05 – Estudiante accede a /api/admin/materias → 403
 *  SM06 – Docente accede a /api/admin/materias → 403
 *  SM07 – POST /api/auth/logout con token válido → 200
 *  SM08 – Token JWT sigue siendo válido tras logout (backend stateless)
 *  SM09 – Estudiante accede a sus propias inscripciones → 200
 *  SM10 – Docente accede a rutas de docente → 200
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("SM – Manejo de sesión y control de acceso por rol")
class SessionManagementTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Tokens reutilizables entre tests
    private static String adminToken;
    private static String estudianteToken;
    private static String docenteToken;
    private static String tokenParaLogout;

    private static final String ADMIN_EMAIL    = "admin@decanato.edu";
    private static final String ADMIN_PASS     = "Admin1234";
    private static final String STU_EMAIL      = "sm_stu@test.com";
    private static final String STU_PASS       = "SmStu1234";
    private static final String DOC_EMAIL      = "sm_doc@test.com";
    private static final String DOC_PASS       = "SmDoc1234";

    @BeforeEach
    void setUpMvc() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String auth(String token) { return "Bearer " + token; }
    private String json(Object o) throws Exception { return objectMapper.writeValueAsString(o); }

    private String loginAndExtractToken(String email, String pass) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", pass))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    /** Registra un estudiante via admin y devuelve su token. */
    private String crearEstudianteYLogin(String adminTok, String email, String pass) throws Exception {
        mockMvc.perform(post("/api/admin/usuarios")
            .header("Authorization", auth(adminTok))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "nombre", "SM", "apellido", "Estudiante",
                "email", email, "password", pass, "rol", "ESTUDIANTE"
            ))));
        return loginAndExtractToken(email, pass);
    }

    /** Registra un docente via admin y devuelve su token. */
    private String crearDocenteYLogin(String adminTok, String email, String pass) throws Exception {
        mockMvc.perform(post("/api/admin/usuarios")
            .header("Authorization", auth(adminTok))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "nombre", "SM", "apellido", "Docente",
                "email", email, "password", pass, "rol", "DOCENTE"
            ))));
        return loginAndExtractToken(email, pass);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM01 – Login válido → 200 + token presente
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("SM01 – Login válido devuelve 200 y token JWT")
    void sm01_loginValido() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASS))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.role").value("ADMINISTRADOR"))
            .andReturn();

        adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
                                 .get("token").asText();
        Assertions.assertNotNull(adminToken, "El token no debe ser null");
        Assertions.assertFalse(adminToken.isBlank(), "El token no debe estar vacío");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM02 – Ruta protegida SIN token → 401
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("SM02 – Ruta protegida sin token devuelve 401")
    void sm02_sinToken() throws Exception {
        mockMvc.perform(get("/api/admin/materias"))
            .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM03 – Token corrupto/inválido → 401
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("SM03 – Token inválido/corrupto devuelve 401")
    void sm03_tokenInvalido() throws Exception {
        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", "Bearer esto.no.es.un.jwt.valido"))
            .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM04 – Admin accede a ruta admin → 200
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("SM04 – Admin accede a /api/admin/materias → 200")
    void sm04_adminAccedeRutaAdmin() throws Exception {
        Assumptions.assumeTrue(adminToken != null, "adminToken requerido de SM01");

        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", auth(adminToken)))
            .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM05 – Estudiante accede a ruta admin → 403
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("SM05 – Estudiante accede a /api/admin/materias → 403")
    void sm05_estudianteAccedeRutaAdmin() throws Exception {
        Assumptions.assumeTrue(adminToken != null, "adminToken requerido de SM01");

        estudianteToken = crearEstudianteYLogin(adminToken, STU_EMAIL, STU_PASS);
        Assumptions.assumeTrue(estudianteToken != null, "No se pudo crear estudiante");

        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", auth(estudianteToken)))
            .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM06 – Docente accede a ruta admin → 403
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("SM06 – Docente accede a /api/admin/materias → 403")
    void sm06_docenteAccedeRutaAdmin() throws Exception {
        Assumptions.assumeTrue(adminToken != null, "adminToken requerido de SM01");

        docenteToken = crearDocenteYLogin(adminToken, DOC_EMAIL, DOC_PASS);
        Assumptions.assumeTrue(docenteToken != null, "No se pudo crear docente");

        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", auth(docenteToken)))
            .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM07 – POST /api/auth/logout con token válido → 200
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    @DisplayName("SM07 – Logout con token válido → 200 y mensaje de confirmación")
    void sm07_logoutExitoso() throws Exception {
        // Obtenemos un token específico para el logout (no queremos invalidar adminToken)
        tokenParaLogout = loginAndExtractToken(ADMIN_EMAIL, ADMIN_PASS);
        Assumptions.assumeTrue(tokenParaLogout != null, "No se pudo obtener token para logout");

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", auth(tokenParaLogout)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(containsString("cerrada")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM08 – Token sigue siendo válido tras logout (el backend es stateless)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @DisplayName("SM08 – Token JWT sigue siendo válido tras logout (backend stateless, front elimina token)")
    void sm08_tokenPostLogoutSigueValido() throws Exception {
        Assumptions.assumeTrue(tokenParaLogout != null, "tokenParaLogout requerido de SM07");

        // El backend es stateless: el token JWT no se revoca en el servidor.
        // El logout es responsabilidad del front (elimina el token del storage).
        // El mismo token aún debe ser aceptado por el backend.
        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", auth(tokenParaLogout)))
            .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM09 – Estudiante accede a sus inscripciones → 200
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(9)
    @DisplayName("SM09 – Estudiante accede a /api/inscripciones/mis-inscripciones → 200")
    void sm09_estudianteAccedeInscripciones() throws Exception {
        Assumptions.assumeTrue(estudianteToken != null, "estudianteToken requerido de SM05");

        mockMvc.perform(get("/api/inscripciones/mis-inscripciones")
                .header("Authorization", auth(estudianteToken)))
            .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SM10 – Docente accede a sus rutas → 200
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("SM10 – Docente accede a ruta de docente (no admin) y NO recibe 403")
    void sm10_docenteAccedeRutaDocente() throws Exception {
        Assumptions.assumeTrue(docenteToken != null, "docenteToken requerido de SM06");

        // /api/docente/materias/{id}/inscripciones: docente tiene permiso → puede ser 200 ó 404
        // pero NUNCA 401 ni 403 (eso verificaría que el rol es reconocido correctamente)
        int status = mockMvc.perform(get("/api/docente/materias/9999/inscripciones")
                .header("Authorization", auth(docenteToken)))
            .andReturn().getResponse().getStatus();

        // Aceptamos 200 (lista vacía) o 404 (materia no existe), pero NO 401/403
        Assertions.assertNotEquals(401, status, "Docente no debe recibir 401 en ruta docente");
        Assertions.assertNotEquals(403, status, "Docente no debe recibir 403 en ruta docente");
    }
}
