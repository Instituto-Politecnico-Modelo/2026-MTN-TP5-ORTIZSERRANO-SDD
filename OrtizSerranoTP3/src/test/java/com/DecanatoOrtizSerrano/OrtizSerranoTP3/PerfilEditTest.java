package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import com.fasterxml.jackson.databind.JsonNode;
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
 * PE – Flujo de edición de perfil (integración)
 *
 * Usa H2 in-memory + Spring Boot completo + MockMvc.
 * DataInitializer siembra admin@decanato.edu / Admin1234 al arrancar.
 *
 *  PE01 – GET  /api/auth/me          → 200 con datos del usuario
 *  PE02 – GET  /api/auth/me          → 401 sin token
 *  PE03 – PUT  /api/auth/update      → 200 actualiza nombre y apellido
 *  PE04 – PUT  /api/auth/update      → cambio reflejado en GET /me
 *  PE05 – PUT  /api/auth/update      → 200 cambia contraseña y puede hacer login con la nueva
 *  PE06 – PUT  /api/auth/update      → 400 email duplicado (ya existe otro usuario con ese email)
 *  PE07 – PUT  /api/auth/update      → 400 nombre demasiado corto (validación Bean)
 *  PE08 – PUT  /api/auth/update      → 200 cambia email y puede hacer login con el nuevo
 *  PE09 – DELETE /api/auth/delete    → 200 baja lógica; usuario no puede volver a hacer login
 *  PE10 – PUT /api/auth/reactivate/{id} → 200 admin reactiva usuario; puede hacer login de nuevo
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("PE – Flujo de edición de perfil")
class PerfilEditTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Estado compartido entre tests (el orden importa)
    private static String adminToken;
    private static String userToken;
    private static Long   userId;

    private static final String ADMIN_EMAIL = "admin@decanato.edu";
    private static final String ADMIN_PASS  = "Admin1234";

    /** Email / contraseña del usuario de prueba que editaremos */
    private static final String USER_EMAIL      = "pe_user@test.com";
    private static final String USER_PASS       = "PeUser1234";
    private static final String USER_NOMBRE     = "Perfil";
    private static final String USER_APELLIDO   = "Edit";

    // Segundo usuario para probar conflicto de email
    private static final String USER2_EMAIL = "pe_otro@test.com";
    private static final String USER2_PASS  = "PeOtro1234";

    @BeforeEach
    void setUpMvc() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String auth(String token) {
        return "Bearer " + token;
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private String loginAndGetToken(String email, String pass) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", pass))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private void crearUsuario(String adminTok, String email, String pass,
                              String nombre, String apellido, String rol) throws Exception {
        mockMvc.perform(post("/api/admin/usuarios")
            .header("Authorization", auth(adminTok))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "nombre", nombre, "apellido", apellido,
                "email", email, "password", pass, "rol", rol
            ))));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE01 – GET /api/auth/me con token válido → 200 + datos del usuario
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("PE01 – GET /api/auth/me devuelve 200 con datos del usuario")
    void pe01_getMeConToken() throws Exception {
        // Obtenemos token de admin (creado por DataInitializer)
        adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASS);

        // Creamos usuario de prueba principal
        crearUsuario(adminToken, USER_EMAIL, USER_PASS, USER_NOMBRE, USER_APELLIDO, "ESTUDIANTE");
        // Creamos segundo usuario para test de email duplicado
        crearUsuario(adminToken, USER2_EMAIL, USER2_PASS, "Otro", "Usuario", "ESTUDIANTE");

        userToken = loginAndGetToken(USER_EMAIL, USER_PASS);

        // Obtenemos el id del usuario para tests posteriores
        MvcResult me = mockMvc.perform(get("/api/auth/me")
                .header("Authorization", auth(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(USER_EMAIL))
            .andExpect(jsonPath("$.nombre").value(USER_NOMBRE))
            .andExpect(jsonPath("$.apellido").value(USER_APELLIDO))
            .andExpect(jsonPath("$.activo").value(true))
            .andReturn();

        JsonNode body = objectMapper.readTree(me.getResponse().getContentAsString());
        userId = body.get("idUsuario").asLong();
        Assertions.assertNotNull(userId, "ID del usuario no debe ser null");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE02 – GET /api/auth/me sin token → 401
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("PE02 – GET /api/auth/me sin token devuelve 401")
    void pe02_getMeSinToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE03 – PUT /api/auth/update actualiza nombre y apellido → 200
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("PE03 – PUT /api/auth/update actualiza nombre y apellido → 200")
    void pe03_actualizaNombreApellido() throws Exception {
        Assumptions.assumeTrue(userToken != null, "userToken requerido de PE01");

        mockMvc.perform(put("/api/auth/update")
                .header("Authorization", auth(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre", "NuevoNombre", "apellido", "NuevoApellido"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(containsString("actualizado")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE04 – El cambio de nombre/apellido se refleja en GET /me
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("PE04 – Nombre y apellido nuevos visibles en GET /api/auth/me")
    void pe04_cambioReflejadoEnMe() throws Exception {
        Assumptions.assumeTrue(userToken != null, "userToken requerido de PE01");

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", auth(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("NuevoNombre"))
            .andExpect(jsonPath("$.apellido").value("NuevoApellido"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE05 – Cambio de contraseña; login con la nueva funciona
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("PE05 – Cambio de contraseña; login con nueva contraseña → 200")
    void pe05_cambioContrasena() throws Exception {
        Assumptions.assumeTrue(userToken != null, "userToken requerido de PE01");

        String nuevaPass = "NuevaClave99";

        // Cambiamos la contraseña
        mockMvc.perform(put("/api/auth/update")
                .header("Authorization", auth(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("password", nuevaPass))))
            .andExpect(status().isOk());

        // Verificamos que la contraseña vieja ya NO funciona
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", USER_EMAIL, "password", USER_PASS))))
            .andExpect(status().isUnauthorized());

        // Verificamos que la contraseña nueva SÍ funciona
        String nuevoToken = loginAndGetToken(USER_EMAIL, nuevaPass);
        Assertions.assertNotNull(nuevoToken, "Login con nueva contraseña debe devolver token");
        Assertions.assertFalse(nuevoToken.isBlank());

        // Actualizamos userToken para los tests siguientes
        userToken = nuevoToken;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE06 – Email duplicado → 400
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("PE06 – PUT /api/auth/update con email ya existente → 400")
    void pe06_emailDuplicado() throws Exception {
        Assumptions.assumeTrue(userToken != null, "userToken requerido de PE01");

        // Intentamos cambiar al email del segundo usuario (que ya existe)
        mockMvc.perform(put("/api/auth/update")
                .header("Authorization", auth(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", USER2_EMAIL))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("email")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE07 – Nombre demasiado corto (< 2 chars) → 400 validación Bean
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    @DisplayName("PE07 – Nombre de 1 carácter viola @Size → 400")
    void pe07_nombreDemasiadoCorto() throws Exception {
        Assumptions.assumeTrue(userToken != null, "userToken requerido de PE01");

        mockMvc.perform(put("/api/auth/update")
                .header("Authorization", auth(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre", "X"))))   // 1 char < min=2
            .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE08 – Cambio de email; login con el nuevo email funciona
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @DisplayName("PE08 – Cambio de email; login con nuevo email → 200")
    void pe08_cambioEmail() throws Exception {
        Assumptions.assumeTrue(userToken != null, "userToken requerido de PE05");

        String nuevoEmail = "pe_nuevo@test.com";
        String passActual = "NuevaClave99"; // contraseña establecida en PE05

        // Cambiamos el email
        mockMvc.perform(put("/api/auth/update")
                .header("Authorization", auth(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", nuevoEmail))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(containsString("actualizado")));

        // Login con el nuevo email funciona
        String tokenNuevoEmail = loginAndGetToken(nuevoEmail, passActual);
        Assertions.assertNotNull(tokenNuevoEmail);
        Assertions.assertFalse(tokenNuevoEmail.isBlank());

        // Email viejo ya no puede hacer login
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", USER_EMAIL, "password", passActual))))
            .andExpect(status().isUnauthorized());

        userToken = tokenNuevoEmail;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE09 – DELETE /api/auth/delete → baja lógica; login bloqueado
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(9)
    @DisplayName("PE09 – Baja lógica; usuario no puede iniciar sesión después")
    void pe09_bajaLogica() throws Exception {
        Assumptions.assumeTrue(userToken != null, "userToken requerido de PE08");

        String nuevoEmail  = "pe_nuevo@test.com";
        String passActual  = "NuevaClave99";

        // Baja lógica
        mockMvc.perform(delete("/api/auth/delete")
                .header("Authorization", auth(userToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(containsString("desactivado")));

        // Login debe fallar (usuario inactivo → UserDetailsServiceImpl lanza excepción)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", nuevoEmail, "password", passActual))))
            .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PE10 – Admin reactiva usuario; puede hacer login nuevamente
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("PE10 – Admin reactiva usuario; login vuelve a funcionar → 200")
    void pe10_reactivarUsuario() throws Exception {
        Assumptions.assumeTrue(adminToken != null, "adminToken requerido de PE01");
        Assumptions.assumeTrue(userId   != null, "userId requerido de PE01");

        String nuevoEmail = "pe_nuevo@test.com";
        String passActual = "NuevaClave99";

        // Admin reactiva al usuario
        mockMvc.perform(put("/api/auth/reactivate/" + userId)
                .header("Authorization", auth(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(containsString("reactivado")));

        // Ahora el usuario puede volver a hacer login
        String tokenReactivado = loginAndGetToken(nuevoEmail, passActual);
        Assertions.assertNotNull(tokenReactivado, "Login post-reactivación debe funcionar");
        Assertions.assertFalse(tokenReactivado.isBlank());

        // Y puede consultar su perfil
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", auth(tokenReactivado)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(true));
    }
}
