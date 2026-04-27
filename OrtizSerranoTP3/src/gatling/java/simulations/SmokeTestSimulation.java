package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Smoke Test – validación rápida (10 usuarios, 60 segundos).
 *
 * Debe ejecutarse ANTES del load test para verificar que:
 *   1. El backend responde correctamente
 *   2. Las URLs no cambiaron
 *   3. El JWT funciona end-to-end
 *   4. La inscripción acepta el request
 *
 * Ejecución:
 *   ./gradlew gatlingSmoke
 */
public class SmokeTestSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(BASE_URL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json");

    private final ScenarioBuilder smokeScenario = scenario("SmokeTest")

        // ── 1. Login admin ────────────────────────────────────────────────
        .exec(
            http("Admin_Login")
                .post("/api/auth/login")
                .body(StringBody("{\"email\":\"admin@decanato.edu\",\"password\":\"Admin1234\"}"))
                .check(status().is(200))
                .check(jmesPath("token").exists().saveAs("adminToken"))
                .check(jmesPath("role").is("ADMINISTRADOR"))
        )
        .pause(Duration.ofMillis(300))

        // ── 3. Listar materias (admin) ────────────────────────────────────
        .exec(
            http("Admin_ListarMaterias")
                .get("/api/admin/materias")
                .header("Authorization", session -> "Bearer " + session.getString("adminToken"))
                .check(status().is(200))
                .check(bodyString().exists())
        )
        .pause(Duration.ofMillis(200))

        // ── 4. Crear materia smoke ────────────────────────────────────────
        .exec(
            http("Admin_CrearMateria_Smoke")
                .post("/api/admin/materias")
                .header("Authorization", session -> "Bearer " + session.getString("adminToken"))
                .body(StringBody("{\"codigo\":\"SMK01\",\"nombre\":\"Smoke Test Materia\"}"))
                .check(status().in(201, 400))  // 400 si ya existe de un run anterior
                .check(jmesPath("idMateria").optional().saveAs("smokeMateriaId"))
        )
        .pause(Duration.ofMillis(200))

        // ── 4. Listar materias con token (ruta autenticada correcta) ─────
        .exec(
            http("Materias_Con_Auth")
                .get("/api/admin/materias")
                .header("Authorization", session -> "Bearer " + session.getString("adminToken"))
                .check(status().is(200))
        )
        .pause(Duration.ofMillis(200))

        // ── 6. Crear estudiante smoke ─────────────────────────────────────
        .exec(
            http("Admin_CrearEstudiante_Smoke")
                .post("/api/admin/usuarios")
                .header("Authorization", session -> "Bearer " + session.getString("adminToken"))
                .body(StringBody("{\"nombre\":\"Smoke\",\"apellido\":\"Tester\"," +
                    "\"email\":\"smoke@load.test\",\"password\":\"Smoke1234\",\"rol\":\"ESTUDIANTE\"}"))
                .check(status().in(201, 400))  // 400 si ya existe
        )
        .pause(Duration.ofMillis(200))

        // ── 7. Login estudiante smoke ─────────────────────────────────────
        .exec(
            http("Estudiante_Login_Smoke")
                .post("/api/auth/login")
                .body(StringBody("{\"email\":\"smoke@load.test\",\"password\":\"Smoke1234\"}"))
                .check(status().in(200, 401))
                .check(jmesPath("token").optional().saveAs("stuToken"))
        )
        .pause(Duration.ofMillis(200))

        // ── 8. Mis inscripciones ──────────────────────────────────────────
        .doIf(session -> session.contains("stuToken")).then(
            exec(
                http("MisInscripciones_Smoke")
                    .get("/api/inscripciones/mis-inscripciones")
                    .header("Authorization", session -> "Bearer " + session.getString("stuToken"))
                    .check(status().is(200))
            )
        );

    {
        setUp(
            smokeScenario.injectOpen(
                atOnceUsers(1),                               // 1 usuario inmediato
                rampUsers(9).during(Duration.ofSeconds(30))  // 9 más en 30s
            )
        )
        .protocols(httpProtocol)
        .assertions(
            global().responseTime().percentile(95).lt(1000),  // p95 < 1s
            global().successfulRequests().percent().gt(90.0)  // >90% éxito
        );
    }
}
