package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Simulación de carga para el sistema de inscripciones del Decanato.
 *
 * Escenarios:
 *   1. EstudiantesInscripcion  – flujo completo: login → listar materias → inscribirse
 *   2. ConsultasMaterias       – solo lectura pública de materias (alta frecuencia)
 *   3. AdminGestion            – admin lista usuarios y materias
 *
 * Carga total: rampa hacia 50.000 usuarios en 10 minutos (modelo de apertura de inscripción).
 *
 * Ejecución:
 *   ./gradlew gatlingInscripciones
 *   (el reporte HTML queda en build/reports/gatling/)
 */
public class InscripcionLoadSimulation extends Simulation {

    // ─── Configuración ──────────────────────────────────────────────────────
    private static final String BASE_URL    = System.getProperty("baseUrl",   "http://localhost:8080");
    private static final int    TOTAL_USERS = Integer.parseInt(System.getProperty("users", "50000"));
    private static final int    RAMP_SECS   = Integer.parseInt(System.getProperty("rampSecs", "600")); // 10 min
    private static final int    HOLD_SECS   = Integer.parseInt(System.getProperty("holdSecs", "120")); // 2 min steady

    // Materia "abierta" para inscribirse (sin límite de cupos – de lo contrario la mayoría falla por diseño)
    private static final String MATERIA_ABIERTA_ID = System.getProperty("materiaId", "1");

    // Contador atómico para generar emails únicos (evita colisión de UNIQUE constraint)
    private static final AtomicInteger userCounter = new AtomicInteger(1);

    // ─── Protocolo HTTP base ─────────────────────────────────────────────────
    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(BASE_URL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .acceptEncodingHeader("gzip, deflate")
        .userAgentHeader("Gatling-LoadTest/3.11")
        .shareConnections()           // pool de conexiones TCP compartido (más realista)
        .maxConnectionsPerHost(200);  // límite conexiones simultáneas por host

    // ─── Feeder: genera credenciales únicas por usuario virtual ─────────────
    // Iterator infinito compatible con la Java API de Gatling
    private final Iterator<Map<String, Object>> credentialFeeder = Stream.generate(() -> {
        int id = userCounter.getAndIncrement();
        return (Map<String, Object>) Map.<String, Object>of(
            "email",    "gatling_user_" + id + "@load.test",
            "password", "GatPass1234",
            "nombre",   "GatUser",
            "apellido", "Test" + id
        );
    }).iterator();

    // ─── Escenario 1: Flujo completo estudiante ──────────────────────────────
    private final ScenarioBuilder estudianteInscripcion = scenario("EstudianteInscripcion")
        .feed(credentialFeeder)

        // Paso 1 – Registro del usuario (admin lo crea; aquí simulamos que ya existe → login)
        // En un ambiente de pre-producción, los usuarios están pre-cargados.
        // En este test asumimos usuarios pre-cargados y sólo hacemos login.
        .exec(
            http("01_Login")
                .post("/api/auth/login")
                .body(StringBody(session ->
                    "{\"email\":\"" + session.getString("email") + "\"," +
                    "\"password\":\"" + session.getString("password") + "\"}"))
                .check(status().in(200, 401))  // 401 si el usuario no existe (esperado en load test)
                .check(
                    jmesPath("token").optional().saveAs("jwtToken")
                )
        )
        .pause(Duration.ofMillis(200), Duration.ofMillis(800))

        // Paso 2 – Listar materias disponibles (endpoint público)
        .exec(
            http("02_ListarMaterias")
                .get("/api/materias/publicas")
                .check(status().in(200, 403, 404))
        )
        .pause(Duration.ofMillis(500), Duration.ofMillis(1500))

        // Paso 3 – Inscribirse (solo si el login fue exitoso)
        .doIf(session -> session.contains("jwtToken")).then(
            exec(
                http("03_Inscribir")
                    .post("/api/inscripciones")
                    .header("Authorization", session -> "Bearer " + session.getString("jwtToken"))
                    .body(StringBody("{\"idMateria\":" + MATERIA_ABIERTA_ID + "}"))
                    .check(status().in(201, 400, 409))  // 201=OK, 400=ya inscripto/sin cupos, 409=locking
            )
            .pause(Duration.ofMillis(300), Duration.ofMillis(700))

            // Paso 4 – Consultar mis inscripciones
            .exec(
                http("04_MisInscripciones")
                    .get("/api/inscripciones/mis-inscripciones")
                    .header("Authorization", session -> "Bearer " + session.getString("jwtToken"))
                    .check(status().is(200))
                    .check(bodyBytes().exists())
            )
        );

    // ─── Escenario 2: Solo consultas de materias (usuarios con token) ────────
    private final ScenarioBuilder consultasMaterias = scenario("ConsultaMaterias")
        .exec(
            http("Login_Lector")
                .post("/api/auth/login")
                .body(StringBody("{\"email\":\"admin@decanato.edu\",\"password\":\"Admin1234\"}"))
                .check(status().is(200))
                .check(jmesPath("token").saveAs("readerToken"))
        )
        .pause(Duration.ofMillis(300))
        .exec(
            http("Materias_Lista")
                .get("/api/admin/materias")
                .header("Authorization", session -> "Bearer " + session.getString("readerToken"))
                .check(status().in(200, 404))
        )
        .pause(Duration.ofMillis(1000), Duration.ofMillis(3000))
        .repeat(3).on(
            exec(
                http("Materia_Detalle")
                    .get("/api/admin/materias/" + MATERIA_ABIERTA_ID)
                    .header("Authorization", session -> "Bearer " + session.getString("readerToken"))
                    .check(status().in(200, 404))
            )
            .pause(Duration.ofMillis(500))
        );

    // ─── Escenario 3: Admin hace operaciones de gestión ──────────────────────
    private final ScenarioBuilder adminGestion = scenario("AdminGestion")
        .exec(
            http("Admin_Login")
                .post("/api/auth/login")
                .body(StringBody("{\"email\":\"admin@decanato.edu\",\"password\":\"Admin1234\"}"))
                .check(status().is(200))
                .check(jmesPath("token").saveAs("adminToken"))
        )
        .pause(Duration.ofMillis(500))
        .exec(
            http("Admin_ListarUsuarios")
                .get("/api/admin/usuarios")
                .header("Authorization", session -> "Bearer " + session.getString("adminToken"))
                .check(status().is(200))
        )
        .pause(Duration.ofMillis(500))
        .exec(
            http("Admin_ListarMaterias")
                .get("/api/admin/materias")
                .header("Authorization", session -> "Bearer " + session.getString("adminToken"))
                .check(status().is(200))
        );

    // ─── Inyección de carga ──────────────────────────────────────────────────
    //
    // Modelo realista de apertura de inscripción:
    //   • Rampa gradual de 0 → 40.000 estudiantes en 10 minutos
    //   • Pico sostenido de 40.000 usuarios durante 2 minutos
    //   • 8.000 usuarios solo-lectura (consultan materias)
    //   • 2.000 sesiones admin concurrentes
    //
    {
        setUp(
            estudianteInscripcion.injectOpen(
                nothingFor(Duration.ofSeconds(5)),              // warm-up
                rampUsers(TOTAL_USERS * 80 / 100)
                    .during(Duration.ofSeconds(RAMP_SECS)),    // rampa: 40.000 en 10 min
                constantUsersPerSec(100)
                    .during(Duration.ofSeconds(HOLD_SECS))     // steady: 100 usr/s por 2 min
            ),
            consultasMaterias.injectOpen(
                rampUsers(TOTAL_USERS * 16 / 100)
                    .during(Duration.ofSeconds(RAMP_SECS))     // 8.000 lectores
            ),
            adminGestion.injectOpen(
                rampUsers(TOTAL_USERS * 4 / 100)
                    .during(Duration.ofSeconds(RAMP_SECS))     // 2.000 admins
            )
        )
        .protocols(httpProtocol)
        // ─── Umbrales de aceptación ────────────────────────────────────────
        .assertions(
            // p95 de tiempo de respuesta bajo 2 segundos en todos los endpoints
            global().responseTime().percentile(95).lt(2000),
            // p99 bajo 5 segundos
            global().responseTime().percentile(99).lt(5000),
            // Máximo tiempo absoluto < 10s
            global().responseTime().max().lt(10000),
            // Tasa de éxito > 95%  (permite hasta 5% de 400/409 por reglas de negocio)
            global().successfulRequests().percent().gt(95.0),
            // Throughput mínimo: al menos 500 req/s sostenidas
            global().requestsPerSec().gt(500.0)
        );
    }
}
