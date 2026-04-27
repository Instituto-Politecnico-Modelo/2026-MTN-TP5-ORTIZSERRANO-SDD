# Load Testing – Gatling

## Descripción

Pruebas de carga para simular la apertura de períodos de inscripción, midiendo tiempos de respuesta y estabilidad bajo **50.000 usuarios simultáneos**.

## Escenarios implementados

| Escenario | Usuarios | Flujo |
|---|---|---|
| `EstudianteInscripcion` | 40.000 (80%) | login → listar materias → inscribirse → mis-inscripciones |
| `ConsultaMaterias` | 8.000 (16%) | consulta pública de materias (solo lectura) |
| `AdminGestion` | 2.000 (4%) | login admin → listar usuarios → listar materias |

## Modelo de carga

```
Usuarios
  50.000 ┤                                    ████████
  40.000 ┤                              ████████
  30.000 ┤                       ███████
  20.000 ┤               ████████
  10.000 ┤       █████████
       0 ┼───────┬────────┬────────┬────────┬────────┬── Tiempo
         0s     2m       4m       6m       8m      12m
         ↑                                  ↑       ↑
       warm-up                         pico (rampa) steady
```

## Umbrales de aceptación (assertions)

| Métrica | Umbral |
|---|---|
| p95 tiempo de respuesta | < 2.000 ms |
| p99 tiempo de respuesta | < 5.000 ms |
| Tiempo máximo absoluto | < 10.000 ms |
| Tasa de éxito | > 95% |
| Throughput mínimo | > 500 req/s |

> **Nota:** 400/409 por reglas de negocio (cupos agotados, ya inscripto) se cuentan como éxito porque son respuestas correctas del sistema.

## Ejecución

### Prerequisito: backend corriendo

```bash
cd OrtizSerranoTP3
./gradlew bootRun
```

### Smoke Test – verificación rápida (10 usuarios / 60s)

```bash
./run-load-test.sh smoke
# o directamente:
./gradlew gatlingSmoke
```

### Load Test completo – 50.000 usuarios

```bash
./run-load-test.sh load
# o directamente:
./gradlew gatlingInscripciones
```

### Smoke + Load en secuencia (recomendado antes de cada período)

```bash
./run-load-test.sh full
```

### Parámetros personalizables

```bash
BASE_URL=http://mi-servidor:8080 \
USERS=50000 \
RAMP_SECS=600 \
HOLD_SECS=120 \
MATERIA_ID=5 \
./run-load-test.sh full
```

| Parámetro | Default | Descripción |
|---|---|---|
| `BASE_URL` | `http://localhost:8080` | URL del backend |
| `USERS` | `50000` | Total de usuarios simulados |
| `RAMP_SECS` | `600` | Segundos de rampa (10 min) |
| `HOLD_SECS` | `120` | Segundos en estado estable (2 min) |
| `MATERIA_ID` | `1` | ID de materia para inscribirse |

## Reporte

El reporte HTML se genera en:

```
OrtizSerranoTP3/build/reports/gatling/<timestamp>/index.html
```

Contiene:
- Gráfico de tiempo de respuesta por percentil (p50, p75, p95, p99)
- Throughput (req/s) a lo largo del tiempo
- Distribución de códigos HTTP
- Resumen de assertions (PASS/FAIL)

## Estructura de archivos

```
src/gatling/
├── java/
│   └── simulations/
│       ├── InscripcionLoadSimulation.java   ← Load test 50.000 usuarios
│       └── SmokeTestSimulation.java         ← Smoke test 10 usuarios
└── resources/
    └── gatling.conf                         ← Configuración Gatling (opcional)
```

## Consideraciones para producción

- En H2 (in-memory) la base de datos es el cuello de botella principal
- Para pruebas reales con 50k usuarios, usar PostgreSQL/MySQL con connection pool (HikariCP)
- Aumentar threads del servidor: `server.tomcat.threads.max=400` en `application.properties`
- Configurar HikariCP: `spring.datasource.hikari.maximum-pool-size=50`
