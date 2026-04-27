package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.InscripcionRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.util.Base64;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  SecurityTest – Suite de Seguridad
 * ═══════════════════════════════════════════════════════════════════
 *
 *  Verifica:
 *   A) Protección por roles JWT (endpoints admin, docente, estudiante)
 *   B) Integridad del token (firma, expiración, algoritmo none, clave falsa)
 *   C) Aislamiento de datos entre usuarios (IDOR – Insecure Direct Object Reference)
 *   D) Inyección SQL / input malicioso en parámetros y body
 *   E) Acceso sin token a recursos protegidos
 *
 *  Orden de ejecución: los tests A-E son independientes entre sí
 *  (cada uno hace login propio), excepto los de IDOR que comparten estado.
 * ═══════════════════════════════════════════════════════════════════
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Suite de Seguridad – JWT, roles e integridad de datos")
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Tokens y IDs compartidos entre tests de IDOR ──────────────────────────
    private static String adminToken;
    private static String estudiante1Token;
    private static String estudiante2Token;
    private static Long inscripcionEstudiante1Id;

    // ── Credenciales de admin (seeded por DataInitializer) ───────────────────
    private static final String ADMIN_EMAIL    = "admin@decanato.edu";
    private static final String ADMIN_PASSWORD = "Admin1234";

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String authHeader(String token) {
        return "Bearer " + token;
    }

    /** Hace login y retorna el JWT */
    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn();
        String json = result.getResponse().getContentAsString();
        // Extraer campo "token" del JSON de respuesta
        return objectMapper.readTree(json).get("token").asText();
    }

    /** Crea un estudiante vía admin y retorna su token */
    private String crearEstudianteYLogin(String email, String password) throws Exception {
        if (adminToken == null) {
            adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        }
        String userJson = String.format(
            "{\"nombre\":\"Test\",\"apellido\":\"Seg\",\"email\":\"%s\",\"password\":\"%s\",\"rol\":\"ESTUDIANTE\"}",
            email, password
        );
        mockMvc.perform(post("/api/admin/usuarios")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
            .andReturn(); // puede fallar si ya existe (idempotente)
        return login(email, password);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BLOQUE A – Protección de endpoints por rol
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("A1 – Sin token → 401 en cualquier endpoint protegido")
    void sinToken_endpointProtegido_retorna401() throws Exception {
        mockMvc.perform(get("/api/admin/materias"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/inscripciones/mis-inscripciones"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/docente/materias/1/inscripciones"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/auditoria"))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(2)
    @DisplayName("A2 – Estudiante intenta acceder a /api/admin/** → 403 Forbidden")
    void estudiante_accedeAdmin_retorna403() throws Exception {
        String estToken = crearEstudianteYLogin("sec.a2@test.com", "SecTest12");

        // Admin: listar materias
        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", authHeader(estToken)))
            .andExpect(status().isForbidden());

        // Admin: crear materia
        mockMvc.perform(post("/api/admin/materias")
                .header("Authorization", authHeader(estToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Hack\",\"codigo\":\"HACK01\",\"anio\":1,\"cuatrimestre\":1,\"cuposMaximos\":30}"))
            .andExpect(status().isForbidden());

        // Admin: crear usuario
        mockMvc.perform(post("/api/admin/usuarios")
                .header("Authorization", authHeader(estToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"apellido\":\"Y\",\"email\":\"x@y.com\",\"password\":\"Test1234\",\"rol\":\"ADMINISTRADOR\"}"))
            .andExpect(status().isForbidden());

        // Admin: auditoría
        mockMvc.perform(get("/api/admin/auditoria")
                .header("Authorization", authHeader(estToken)))
            .andExpect(status().isForbidden());
    }

    @Test @Order(3)
    @DisplayName("A3 – Estudiante intenta acceder a /api/docente/** → 403 Forbidden")
    void estudiante_accedeDocente_retorna403() throws Exception {
        String estToken = crearEstudianteYLogin("sec.a3@test.com", "SecTest12");

        mockMvc.perform(get("/api/docente/materias/1/inscripciones")
                .header("Authorization", authHeader(estToken)))
            .andExpect(status().isForbidden());
    }

    @Test @Order(4)
    @DisplayName("A4 – Admin accede a endpoints de estudiante → 200 (admin tiene acceso total)")
    void admin_accedeEndpointsEstudiante_retorna200() throws Exception {
        if (adminToken == null) adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // /api/inscripciones/mis-inscripciones devuelve 200 para admin también
        mockMvc.perform(get("/api/inscripciones/mis-inscripciones")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk());
    }

    @Test @Order(5)
    @DisplayName("A5 – Token con prefijo incorrecto (sin 'Bearer ') → 401")
    void token_sinPrefijosBearer_retorna401() throws Exception {
        if (adminToken == null) adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Token directo sin "Bearer "
        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", adminToken)) // sin "Bearer "
            .andExpect(status().isUnauthorized());

        // Prefijo incorrecto
        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", "Token " + adminToken))
            .andExpect(status().isUnauthorized());

        // Header vacío
        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", ""))
            .andExpect(status().isUnauthorized());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BLOQUE B – Integridad y manipulación del token JWT
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(10)
    @DisplayName("B1 – Token con firma manipulada → 401 (firma inválida)")
    void token_firmaManipulada_retorna401() throws Exception {
        if (adminToken == null) adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Tomar el token real y cambiar un carácter en el MEDIO de la firma
        // (no el último, ya que en base64url sin padding los últimos bits pueden ser irrelevantes)
        String[] partes = adminToken.split("\\.");
        String firma = partes[2];
        // Cambiar un char en la posición central de la firma
        int medio = firma.length() / 2;
        char original = firma.charAt(medio);
        char reemplazo = (original == 'a') ? 'B' : 'a';
        String firmaRota = firma.substring(0, medio) + reemplazo + firma.substring(medio + 1);
        String tokenManipulado = partes[0] + "." + partes[1] + "." + firmaRota;

        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", authHeader(tokenManipulado)))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(11)
    @DisplayName("B2 – Token con subject manipulado (cambiar email a otro usuario) → 401 o 403")
    void token_payloadManipulado_cambioSubject_retorna401o403() throws Exception {
        // Crear estudiante y obtener su token
        String estToken = crearEstudianteYLogin("sec.b2@test.com", "SecTest12");

        // Decodificar el payload (parte 2)
        String[] partes = estToken.split("\\.");
        String payloadDecoded = new String(Base64.getUrlDecoder().decode(partes[1]));

        // El JWT contiene: {"sub":"sec.b2@test.com","iat":...,"exp":...}
        // Modificar el subject para intentar suplantar al admin
        String payloadModificado = payloadDecoded
            .replace("sec.b2@test.com", "admin@decanato.edu");
        String payloadRecodificado = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payloadModificado.getBytes());

        // Token con subject adulterado pero firma ORIGINAL (calculada sobre el payload original)
        // → la firma no coincide con el nuevo payload → JJWT debe rechazarlo → 401
        String tokenAdulterado = partes[0] + "." + payloadRecodificado + "." + partes[2];

        // Debe ser rechazado con 401 (firma inválida) al intentar acceder a un endpoint admin
        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", authHeader(tokenAdulterado)))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(12)
    @DisplayName("B3 – Token con algoritmo 'none' (header alg:none) → 401")
    void token_algoritmoNone_retorna401() throws Exception {
        // Construir manualmente un token JWT con alg:none (ataque clásico)
        String headerNone = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"sub\":\"admin@decanato.edu\","
                + "\"iat\":1000000000,\"exp\":9999999999}").getBytes());
        // Token sin firma (alg:none)
        String tokenNone = headerNone + "." + payload + ".";

        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", authHeader(tokenNone)))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(13)
    @DisplayName("B4 – Token con clave diferente (firmado con otra clave) → 401")
    void token_claveDiferente_retorna401() throws Exception {
        // Generar manualmente un HS256 con clave "fake" usando Base64
        // Esto simula un atacante que tiene su propia clave pero no la del servidor
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"sub\":\"admin@decanato.edu\","
                + "\"iat\":1000000000,\"exp\":9999999999}").getBytes());
        // Firma falsa (no es el HMAC real con la clave del servidor)
        String firmaFalsa = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("firmaFalsaNoValida12345678901234".getBytes());
        String tokenFalso = header + "." + payload + "." + firmaFalsa;

        mockMvc.perform(get("/api/admin/materias")
                .header("Authorization", authHeader(tokenFalso)))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(14)
    @DisplayName("B5 – Token truncado / malformado → 401")
    void token_malformado_retorna401() throws Exception {
        // Tokens claramente inválidos
        String[] tokensInvalidos = {
            "no.es.jwt",
            "Bearer",
            "eyJhbGciOiJIUzI1NiJ9",   // solo header
            "abc123",
            ""
        };
        for (String t : tokensInvalidos) {
            mockMvc.perform(get("/api/admin/materias")
                    .header("Authorization", "Bearer " + t))
                .andExpect(status().isUnauthorized());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BLOQUE C – Aislamiento de datos entre usuarios (IDOR)
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(20)
    @DisplayName("C0 – Setup IDOR: crear estudiantes e inscripción")
    void idor_setup() throws Exception {
        if (adminToken == null) adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Crear estudiante 1 y 2
        estudiante1Token = crearEstudianteYLogin("sec.idor1@test.com", "SecTest12");
        estudiante2Token = crearEstudianteYLogin("sec.idor2@test.com", "SecTest12");

        // Admin crea una materia con 1 cupo para este test
        String matJson = "{\"nombre\":\"MatSeguridad\",\"codigo\":\"SECU01\","
            + "\"anio\":1,\"cuatrimestre\":1,\"cuposMaximos\":5}";
        MvcResult mRes = mockMvc.perform(post("/api/admin/materias")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(matJson))
            .andReturn();

        String matBody = mRes.getResponse().getContentAsString();
        Long idMateria = objectMapper.readTree(matBody).get("idMateria").asLong();

        // Estudiante 1 se inscribe
        InscripcionRequest req = new InscripcionRequest();
        req.setIdMateria(idMateria);
        MvcResult iRes = mockMvc.perform(post("/api/inscripciones")
                .header("Authorization", authHeader(estudiante1Token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();

        String iBody = iRes.getResponse().getContentAsString();
        inscripcionEstudiante1Id = objectMapper.readTree(iBody).get("idInscripcion").asLong();

        assertNotNull(inscripcionEstudiante1Id, "ID de inscripción debe haberse creado");
    }

    @Test @Order(21)
    @DisplayName("C1 – Estudiante 2 intenta cancelar inscripción de Estudiante 1 → 400 (no autorizado)")
    void idor_estudianteNoPuedeCancelarInscripcionAjena() throws Exception {
        // Estudiante 2 intenta cancelar la inscripción del estudiante 1
        mockMvc.perform(patch("/api/inscripciones/" + inscripcionEstudiante1Id + "/cancelar")
                .header("Authorization", authHeader(estudiante2Token)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsStringIgnoringCase("inscripci")));
    }

    @Test @Order(22)
    @DisplayName("C2 – mis-inscripciones devuelve SOLO las del usuario autenticado (no las de otros)")
    void idor_misInscripcionesAisladasPorUsuario() throws Exception {
        // Estudiante 1: debe ver SU inscripción
        MvcResult r1 = mockMvc.perform(get("/api/inscripciones/mis-inscripciones")
                .header("Authorization", authHeader(estudiante1Token)))
            .andExpect(status().isOk())
            .andReturn();

        // Estudiante 2: no debe ver la inscripción del estudiante 1
        MvcResult r2 = mockMvc.perform(get("/api/inscripciones/mis-inscripciones")
                .header("Authorization", authHeader(estudiante2Token)))
            .andExpect(status().isOk())
            .andReturn();

        String listaEst1 = r1.getResponse().getContentAsString();
        String listaEst2 = r2.getResponse().getContentAsString();

        // La lista del est2 no debe contener el id de inscripción del est1
        assertFalse(
            listaEst2.contains(inscripcionEstudiante1Id.toString()),
            "Estudiante 2 NO debe ver las inscripciones de Estudiante 1"
        );
        // La lista del est1 SÍ debe contener su inscripción
        assertTrue(
            listaEst1.contains(inscripcionEstudiante1Id.toString()),
            "Estudiante 1 SÍ debe ver su propia inscripción"
        );
    }

    @Test @Order(23)
    @DisplayName("C3 – /api/auth/me devuelve SOLO datos del usuario del token (no de otro)")
    void idor_meSoloDevuelveDatosDelTokenPropietario() throws Exception {
        MvcResult r1 = mockMvc.perform(get("/api/auth/me")
                .header("Authorization", authHeader(estudiante1Token)))
            .andExpect(status().isOk())
            .andReturn();

        MvcResult r2 = mockMvc.perform(get("/api/auth/me")
                .header("Authorization", authHeader(estudiante2Token)))
            .andExpect(status().isOk())
            .andReturn();

        String resp1 = r1.getResponse().getContentAsString();
        String resp2 = r2.getResponse().getContentAsString();

        String email1 = objectMapper.readTree(resp1).get("email").asText();
        String email2 = objectMapper.readTree(resp2).get("email").asText();

        assertEquals("sec.idor1@test.com", email1,
            "El /me del estudiante 1 debe devolver su propio email");
        assertEquals("sec.idor2@test.com", email2,
            "El /me del estudiante 2 debe devolver su propio email");
        assertNotEquals(email1, email2,
            "Dos tokens distintos NO deben devolver el mismo usuario");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BLOQUE D – Inyección SQL e inputs maliciosos
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(30)
    @DisplayName("D1 – SQL Injection en login → 401 (no bypass de autenticación)")
    void sqli_enLogin_noBypassea() throws Exception {
        // Payloads clásicos de SQL Injection
        String[] payloads = {
            "admin' --",
            "admin' OR '1'='1",
            "' OR 1=1 --",
            "admin'/*",
            "admin@decanato.edu'; DROP TABLE usuarios; --"
        };
        for (String payload : payloads) {
            String body = objectMapper.writeValueAsString(new LoginRequest(payload, "cualquier"));
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().is4xxClientError()); // 401 o 400
        }
    }

    @Test @Order(31)
    @DisplayName("D2 – SQL Injection en path variable numérico → 400 (tipo incorrecto, no 500)")
    void sqli_enPathVariable_noProduceError500() throws Exception {
        if (adminToken == null) adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Spring MVC convierte el path variable a Long; si el valor no es un número
        // debe lanzar MethodArgumentTypeMismatchException → ahora manejado como 400
        mockMvc.perform(patch("/api/inscripciones/not-a-number/cancelar")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isBadRequest());  // 400 por MethodArgumentTypeMismatchException

        // Path variable no numérico en materias (el handler GET /{id} espera Long)
        mockMvc.perform(get("/api/admin/materias/union-select-all")
                .header("Authorization", authHeader(adminToken)))
            .andExpect(result ->
                assertTrue(result.getResponse().getStatus() < 500,
                    "Debe ser 4xx, no 500, fue: " + result.getResponse().getStatus()));
    }

    @Test @Order(32)
    @DisplayName("D3 – XSS en campos de texto → servidor acepta o valida (no ejecuta script)")
    void xss_enCamposTexto_noRetorna500() throws Exception {
        if (adminToken == null) adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Intentar crear materia con nombre XSS
        String materiaXss = "{\"nombre\":\"<script>alert('xss')</script>\","
            + "\"codigo\":\"XSS01\",\"anio\":1,\"cuatrimestre\":1,\"cuposMaximos\":5}";

        // El servidor debe responder con 201 (almacena el string tal cual, escapa en frontend)
        // o 400 si hay validación. Lo importante: NUNCA 500.
        int status = mockMvc.perform(post("/api/admin/materias")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(materiaXss))
            .andReturn()
            .getResponse()
            .getStatus();

        assertTrue(status == 201 || status == 400,
            "XSS en input no debe provocar error 500, fue: " + status);
    }

    @Test @Order(33)
    @DisplayName("D4 – Payload oversized en body → no provoca error 500")
    void payloadGigante_noProvocaError500() throws Exception {
        // String de 50KB en el campo nombre
        String nombreGigante = "A".repeat(50_000);
        String body = String.format(
            "{\"nombre\":\"%s\",\"codigo\":\"BIG01\",\"anio\":1,\"cuatrimestre\":1,\"cuposMaximos\":5}",
            nombreGigante
        );
        if (adminToken == null) adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        int status = mockMvc.perform(post("/api/admin/materias")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andReturn()
            .getResponse()
            .getStatus();

        // Debe retornar 400 o 413, NUNCA 500
        assertNotEquals(500, status,
            "Un payload gigante no debe provocar error 500 del servidor");
    }

    @Test @Order(34)
    @DisplayName("D5 – JSON malformado en body → 400 Bad Request (no 500)")
    void jsonMalformado_retorna400() throws Exception {
        if (adminToken == null) adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        String[] malformedJsons = {
            "{nombre: faltanComillas}",
            "{{}}",
            "null",
            "<xml>esto no es json</xml>"
        };

        for (String json : malformedJsons) {
            int status = mockMvc.perform(post("/api/admin/materias")
                    .header("Authorization", authHeader(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andReturn()
                .getResponse()
                .getStatus();

            assertNotEquals(500, status,
                "JSON malformado no debe provocar 500, fue: " + status + " con input: " + json);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BLOQUE E – Headers HTTP y seguridad adicional
    // ═════════════════════════════════════════════════════════════════════════

    @Test @Order(40)
    @DisplayName("E1 – /api/auth/login es accesible sin token (endpoint público)")
    void login_esPublico_sinToken() throws Exception {
        // Login con credenciales inválidas devuelve 401, no 404 ni 403
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"no@existe.com\",\"password\":\"mal\"}"))
            .andExpect(status().is4xxClientError());
    }

    @Test @Order(41)
    @DisplayName("E2 – OPTIONS preflight (CORS) → 200 sin token requerido")
    void options_preflight_retorna200() throws Exception {
        mockMvc.perform(options("/api/admin/materias")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
            .andExpect(status().isOk());
    }

    @Test @Order(42)
    @DisplayName("E3 – Swagger UI es accesible sin autenticación")
    void swaggerUi_esPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk());
    }

    @Test @Order(43)
    @DisplayName("E4 – Password no aparece expuesta en respuesta de login")
    void login_noExponeLaPassword() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        assertFalse(responseBody.contains(ADMIN_PASSWORD),
            "La contraseña en texto plano NO debe aparecer en la respuesta del login");
        assertFalse(responseBody.toLowerCase().contains("\"password\""),
            "El campo 'password' no debe ser expuesto en la respuesta del login");
    }

    @Test @Order(44)
    @DisplayName("E5 – Reutilización de token válido en múltiples requests consecutivos")
    void token_esReutilizable_enMultiplesRequests() throws Exception {
        if (adminToken == null) adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // El mismo token debe funcionar en 3 requests seguidas (stateless correctamente)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/admin/materias")
                    .header("Authorization", authHeader(adminToken)))
                .andExpect(status().isOk());
        }
    }
}
